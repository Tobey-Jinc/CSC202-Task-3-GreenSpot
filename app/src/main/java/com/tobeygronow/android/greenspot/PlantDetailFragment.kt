package com.tobeygronow.android.greenspot

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.tobeygronow.android.greenspot.databinding.FragmentPlantDetailBinding
import kotlinx.coroutines.launch
import android.text.format.DateFormat
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.view.doOnLayout
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import java.io.File
import java.util.Date
import kotlin.math.log

private const val DATE_FORMAT = "EEE, MMM, dd"

class PlantDetailFragment : Fragment() {

    private var _binding: FragmentPlantDetailBinding? = null
    private val binding 
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private val args: PlantDetailFragmentArgs by navArgs()

    private val plantDetailViewModel: PlantDetailViewModel by viewModels {
        PlantDetailViewModelFactory(args.plantId)
    }

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { didTakePhoto: Boolean ->
        if (didTakePhoto && photoName != null) {
            plantDetailViewModel.updatePlant { oldPlant ->
                oldPlant.copy(photoFileName = photoName)
            }
        }
    }

    private var photoName: String? = null

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var plant: Plant? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = 
            FragmentPlantDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            plantTitle.doOnTextChanged { text, _, _, _ ->
                plantDetailViewModel.updatePlant { oldPlant ->
                    oldPlant.copy(title = text.toString())
                }
            }

            plantPlace.doOnTextChanged { text, _, _, _ ->
                plantDetailViewModel.updatePlant { oldPlant ->
                    oldPlant.copy(place = text.toString())
                }
            }

            plantSetLocation.setOnClickListener {
                if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(
                        requireContext(),
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d("GPS", "Try to get location")
                    if (GoogleApiAvailability.getInstance()
                            .isGooglePlayServicesAvailable(requireContext()) == ConnectionResult.SUCCESS
                    ) {
                        fusedLocationProviderClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            object : CancellationToken() {
                                override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                                    CancellationTokenSource().token

                                override fun isCancellationRequested() = false
                            }
                        ).addOnSuccessListener { location: Location? ->
                            location?.let {
                                plantDetailViewModel.updatePlant { oldPlant ->
                                    oldPlant.copy(longitude = location.longitude, latitude = location.latitude)
                                }
                                plantLocation.text = "Longitude: ${location.longitude}, Latitude: ${location.latitude}"
                            }
                            Log.d("GPS", "Got location: $location")
                        }
                    }
                }
                else {
                    ActivityCompat.requestPermissions(requireActivity(),
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                        0)
                }
            }

            googleMaps.setOnClickListener {
                val intent = Intent(requireContext(), MapsActivity::class.java)
                intent.putExtra("title", plant!!.title)
                intent.putExtra("longitude", plant!!.longitude)
                intent.putExtra("latitude", plant!!.latitude)
                startActivity(intent)
            }

            plantCamera.setOnClickListener {
                photoName = "IMG_${Date()}.JPG"
                val photoFile = File(requireContext().applicationContext.filesDir,
                    photoName)
                val photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.tobeygronow.android.greenspot.fileprovider",
                    photoFile
                )
                takePhoto.launch(photoUri)
            }

            val captureImageIntent = takePhoto.contract.createIntent(
                requireContext(),
                null
            )
            plantCamera.isEnabled = canResolveIntent(captureImageIntent)

        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                plantDetailViewModel.plant.collect { plant ->
                    plant?.let { updateUi(it) }
                }
            }
        }

        setFragmentResultListener(
            DatePickerFragment.REQUEST_KEY_DATE
        ) { _, bundle ->
            val newDate =
                bundle.getSerializable(DatePickerFragment.BUNDLE_KEY_DATE) as Date
            plantDetailViewModel.updatePlant { it.copy(date = newDate) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUi(plant: Plant) {
        this.plant = plant

        binding.apply {
            if (plantTitle.text.toString() != plant.title) {
                plantTitle.setText(plant.title)
            }

            if (plantPlace.text.toString() != plant.place) {
                plantPlace.setText(plant.place)
            }

            plantDate.text = plant.date.toString()

            plantDate.setOnClickListener {
                findNavController().navigate (
                    PlantDetailFragmentDirections.selectDate(plant.date)
                )
            }

            if (plant.longitude == null || plant.latitude == null) {
                plantLocation.text = "No location set!"
                googleMaps.isEnabled = false
                googleMaps.isClickable = false
            }
            else {
                plantLocation.text = "Longitude: ${plant.longitude}, Latitude: ${plant.latitude}"
                googleMaps.isEnabled = true
                googleMaps.isClickable = true
            }

//            plantReport.setOnClickListener {
//                val reportIntent = Intent(Intent.ACTION_SEND).apply {
//                    type = "text/plain"
//                    putExtra(Intent.EXTRA_TEXT, getPlantReport(plant))
//                    putExtra(
//                        Intent.EXTRA_SUBJECT,
//                        getString(R.string.plant_report_subject)
//                    )
//                }
//                val chooserIntent = Intent.createChooser(
//                    reportIntent,
//                    getString(R.string.send_report)
//                )
//                startActivity(chooserIntent)
//            }

            updatePhoto(plant.photoFileName)
        }
    }

    private fun getPlantReport(plant: Plant): String {
//        val solvedString = if (plant.isSolved) {
//            getString(R.string.plant_report_solved)
//        } else {
//            getString(R.string.plant_report_unsolved)
//        }
//        val dateString = DateFormat.format(DATE_FORMAT, plant.date).toString()
//        val suspectText = if (plant.suspect.isBlank()) {
//            getString(R.string.plant_report_no_suspect)
//        } else {
//            getString(R.string.plant_report_suspect, plant.suspect)
//        }
        return ""
//        return getString(
//            R.string.plant_report,
//            plant.title, dateString, solvedString, suspectText
//        )
    }

    private fun parseContactSelection(contactUri: Uri) {
//        val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
//        val queryCursor = requireActivity().contentResolver
//            .query(contactUri, queryFields, null, null, null)
//        queryCursor?.use { cursor ->
//            if (cursor.moveToFirst()) {
//                val suspect = cursor.getString(0)
//                plantDetailViewModel.updatePlant { oldPlant ->
//                    oldPlant.copy(suspect = suspect)
//                }
//            }
//        }
    }

    private fun canResolveIntent(intent: Intent): Boolean {
        val packageManager: PackageManager = requireActivity().packageManager
        val resolvedActivity: ResolveInfo? =
            packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        return resolvedActivity != null
    }

    private fun updatePhoto(photoFileName: String?) {
        if (binding.plantPhoto.tag != photoFileName) {
            val photoFile = photoFileName?.let {
                File(requireContext().applicationContext.filesDir, it)
            }
            if (photoFile?.exists() == true) {
                binding.plantPhoto.doOnLayout { measuredView ->
                    val scaledBitmap = getScaledBitmap(
                        photoFile.path,
                        measuredView.width,
                        measuredView.height
                    )
                    binding.plantPhoto.setImageBitmap(scaledBitmap)
                    binding.plantPhoto.tag = photoFileName
                    binding.plantPhoto.contentDescription =
                        getString(R.string.plant_photo_image_description)

                }
            } else {
                binding.plantPhoto.setImageBitmap(null)
                binding.plantPhoto.tag = null
                binding.plantPhoto.contentDescription =
                    getString(R.string.plant_photo_no_image_description)
            }
        }
    }
}
