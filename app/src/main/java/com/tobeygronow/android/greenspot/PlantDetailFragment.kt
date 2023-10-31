package com.tobeygronow.android.greenspot

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.doOnLayout
import java.io.File
import java.util.Date

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

    private val selectSuspect = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let { parseContactSelection(it) }
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

            plantSolved.setOnCheckedChangeListener { _, isChecked ->
                plantDetailViewModel.updatePlant { oldPlant ->
                    oldPlant.copy(isSolved = isChecked)
                }
            }

            plantSuspect.setOnClickListener {
                selectSuspect.launch(null)
            }

            val selectSuspectIntent = selectSuspect.contract.createIntent(
                requireContext(),
                null
            )
            plantSuspect.isEnabled = canResolveIntent(selectSuspectIntent)

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
        binding.apply {
            if (plantTitle.text.toString() != plant.title) {
                plantTitle.setText(plant.title)
            }
            plantDate.text = plant.date.toString()

            plantDate.setOnClickListener {
                findNavController().navigate (
                    PlantDetailFragmentDirections.selectDate(plant.date)
                )
            }

            plantSolved.isChecked = plant.isSolved

            plantReport.setOnClickListener {
                val reportIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getPlantReport(plant))
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        getString(R.string.plant_report_subject)
                    )
                }
                val chooserIntent = Intent.createChooser(
                    reportIntent,
                    getString(R.string.send_report)
                )
                startActivity(chooserIntent)
            }

            plantSuspect.text = plant.suspect.ifEmpty {
                getString(R.string.plant_suspect_text)
            }

            updatePhoto(plant.photoFileName)
        }
    }

    private fun getPlantReport(plant: Plant): String {
        val solvedString = if (plant.isSolved) {
            getString(R.string.plant_report_solved)
        } else {
            getString(R.string.plant_report_unsolved)
        }
        val dateString = DateFormat.format(DATE_FORMAT, plant.date).toString()
        val suspectText = if (plant.suspect.isBlank()) {
            getString(R.string.plant_report_no_suspect)
        } else {
            getString(R.string.plant_report_suspect, plant.suspect)
        }
        return getString(
            R.string.plant_report,
            plant.title, dateString, solvedString, suspectText
        )
    }

    private fun parseContactSelection(contactUri: Uri) {
        val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
        val queryCursor = requireActivity().contentResolver
            .query(contactUri, queryFields, null, null, null)
        queryCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                val suspect = cursor.getString(0)
                plantDetailViewModel.updatePlant { oldPlant ->
                    oldPlant.copy(suspect = suspect)
                }
            }
        }
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
