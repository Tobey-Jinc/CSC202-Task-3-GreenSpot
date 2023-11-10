package com.tobeygronow.android.greenspot

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.location.Location
import android.net.Uri
import android.os.Bundle
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
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.view.doOnLayout
import androidx.navigation.NavOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import java.io.File
import java.text.DateFormat
import java.util.Date

private const val DATE_FORMAT = "EEE, MMM, dd"

/**
 * Displays a Plants details, and allows them to be edited
 */
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

    // Update the Plants photo
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

        // Used to access the users location
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

        // Define events
        binding.apply {
            // Set Plant title
            plantTitle.doOnTextChanged { text, _, _, _ ->
                plantDetailViewModel.updatePlant { oldPlant ->
                    oldPlant.copy(title = text.toString())
                }
            }

            // Set Plant place
            plantPlace.doOnTextChanged { text, _, _, _ ->
                plantDetailViewModel.updatePlant { oldPlant ->
                    oldPlant.copy(place = text.toString())
                }
            }

            // Set Plant latitude and longitude
            plantSetLocation.setOnClickListener {
                // Check if required permissions are available
                if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(
                        requireContext(),
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) { // Permission available, try to get location
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
                            // Update the Plants latitude and longitude
                            location?.let {
                                plantDetailViewModel.updatePlant { oldPlant ->
                                    oldPlant.copy(longitude = location.longitude, latitude = location.latitude)
                                }
                            }
                        }
                    }
                }
                else { // Permissions not available... Ask for permission
                    ActivityCompat.requestPermissions(requireActivity(),
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                        0)
                }
            }

            // Display the Plants location with Google Maps
            googleMaps.setOnClickListener {
                // Start activity and send the Plants name, latitude and longitude with intent extras
                val intent = Intent(requireContext(), MapsActivity::class.java)
                intent.putExtra("title", plant!!.title)
                intent.putExtra("longitude", plant!!.longitude)
                intent.putExtra("latitude", plant!!.latitude)
                startActivity(intent)
            }

            // Take a photo
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

            // Display a zoomed in version of the photo
            plantPhoto.setOnClickListener {
                plant!!.photoFileName?.let { dialog ->
                    PlantImageDialogFragment.newInstance(
                        dialog
                    )
                }?.show(childFragmentManager, null)
            }

            val captureImageIntent = takePhoto.contract.createIntent(
                requireContext(),
                null
            )
            plantCamera.isEnabled = canResolveIntent(captureImageIntent)

            // Only display the Delete button if the Plant was not just created
            if (args.justCreated) {
                plantDelete.visibility = View.GONE
            }
            else {
                // Delete the Plant
                plantDelete.setOnClickListener {
                    viewLifecycleOwner.lifecycleScope.launch {
                        // Remove from database
                        plant?.let { PlantRepository.get().removePlant(it) }

                        // Navigate back to list view, and prevent navigation back to this detail view
                        // Credit: https://stackoverflow.com/a/67281386
                        findNavController().navigate(R.id.nav_graph,null,NavOptions.Builder().setPopUpTo(findNavController().graph.startDestinationId, true).build())
                    }
                }
            }
        }

        // Update the fragment details whenever the Plant changes in the database
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                plantDetailViewModel.plant.collect { plant ->
                    plant?.let { updateUi(it) }
                }
            }
        }

        // Update Plant date whenever the DatePickerFragment provides a result
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

    /**
     * Updates the UI to match the information of the specified Plant
     *
     * @param plant The Plant information to be displayed
     */
    private fun updateUi(plant: Plant) {
        this.plant = plant // Store the Plant for easy access

        binding.apply {
            // Update title
            if (plantTitle.text.toString() != plant.title) {
                plantTitle.setText(plant.title)
            }

            // Update place
            if (plantPlace.text.toString() != plant.place) {
                plantPlace.setText(plant.place)
            }

            // Format and update date
            val df = DateFormat.getDateInstance(DateFormat.LONG)
            plantDate.text = df.format(plant.date)

            // Update date fragment event so it displays the Plants date upon being opened
            plantDate.setOnClickListener {
                findNavController().navigate (
                    PlantDetailFragmentDirections.selectDate(plant.date)
                )
            }

            // Display the latitude and longitude if possible
            if (plant.longitude == null || plant.latitude == null) {
                // Not possible so say so, and disable Google Maps button
                plantLocation.text = getString(R.string.plant_null_coordinates)
                googleMaps.isEnabled = false
                googleMaps.isClickable = false
            }
            else {
                // Is possible so display information and enable Google Maps button
                plantLocation.text = getString(R.string.plant_coordinates, plant.latitude, plant.longitude)
                googleMaps.isEnabled = true
                googleMaps.isClickable = true
            }

            // Update the share button event so it displays up to date information
            plantShare.setOnClickListener {
                val reportIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getPlantReport(plant))
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        getString(R.string.plant_share_subject)
                    )
                }
                val chooserIntent = Intent.createChooser(
                    reportIntent,
                    getString(R.string.plant_share_send)
                )
                startActivity(chooserIntent)
            }

            updatePhoto(plant.photoFileName)
        }
    }

    /**
     * Creates a string report of a Plants information
     *
     * @param plant The Plant to get the report information from
     * @return The plant report as a string
     */
    private fun getPlantReport(plant: Plant): String {
        // Title
        val titleString = if (plant.title.isNotEmpty()) {
            getString(R.string.plant_share_title, plant.title)
        } else {
            getString(R.string.plant_share_no_title)
        }

        // Place
        val placeString = if (plant.place.isNotEmpty()) {
            getString(R.string.plant_share_place, plant.place)
        } else {
            getString(R.string.plant_share_no_place)
        }

        // Date
        val df = DateFormat.getDateInstance(DateFormat.LONG)
        val dateString = getString(R.string.plant_share_date, df.format(plant.date))

        // Longitude and Latitude
        val coordinatesString = if (plant.latitude != null && plant.longitude != null) {
            getString(R.string.plant_share_coordinates, plant.latitude, plant.longitude)
        } else {
            getString(R.string.plant_share_no_coordinates)
        }

        // Put everything together
        return getString(
            R.string.plant_share_template,
            titleString, placeString, dateString, coordinatesString
        )
    }

    /**
     * Checks if an Intent can be resolved
     *
     * @param intent The Intent to check
     * @return true if it can be resolved, false otherwise
     */
    private fun canResolveIntent(intent: Intent): Boolean {
        val packageManager: PackageManager = requireActivity().packageManager
        val resolvedActivity: ResolveInfo? =
            packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        return resolvedActivity != null
    }

    /**
     * Display the Plants photo
     *
     * @param photoFileName The file name of the photo to be displayed
     */
    private fun updatePhoto(photoFileName: String?) {
        if (binding.plantPhoto.tag != photoFileName) {
            // Get the photo
            val photoFile = photoFileName?.let {
                File(requireContext().applicationContext.filesDir, it)
            }
            // Only update if the photo exists
            if (photoFile?.exists() == true) {
                binding.plantPhoto.doOnLayout { measuredView ->
                    // Scale the photo
                    val scaledBitmap = getScaledBitmap(
                        photoFile.path,
                        measuredView.width,
                        measuredView.height
                    )
                    binding.plantPhoto.setImageBitmap(scaledBitmap) // Display the photo
                    binding.plantPhoto.tag = photoFileName // Tag it
                    binding.plantPhoto.contentDescription = // Add description
                        getString(R.string.plant_photo_image_description)

                }
            } else {
                // Photo doesn't exist, so display nothing, and tag and describe accordingly
                binding.plantPhoto.setImageBitmap(null)
                binding.plantPhoto.tag = null
                binding.plantPhoto.contentDescription =
                    getString(R.string.plant_photo_no_image_description)
            }
        }
    }
}
