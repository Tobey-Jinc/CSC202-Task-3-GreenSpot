package com.tobeygronow.android.greenspot

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment

/**
 * Displays a zoomed in image
 */
class PlantImageDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Create the DialogFragment layout
        val view = inflater.inflate(R.layout.dialog_fragment_zoomed_image, container, false)
        // Get the layouts ImageView
        val imageView = view.findViewById(R.id.zoomed_image_view) as ImageView

        // Display the photo
        val photoFileName = arguments?.getSerializable("PHOTO_URI") as String
        imageView.setImageBitmap(BitmapFactory.decodeFile(requireContext().filesDir.path + "/" + photoFileName))

        return view
    }

    /**
     * Make PlantImageDialogFragment accessible so PlantDetailFragment can easily create it
     */
    companion object {
        /**
         * Creates a new instance of PlantImageDialogFragment
         *
         * @param photoFileName The photo to be displayed
         * @return The new instance of PlantImageDialogFragment
         */
        fun newInstance(photoFileName: String): PlantImageDialogFragment {
            val fragment = PlantImageDialogFragment()
            val args = Bundle()
            args.putSerializable("PHOTO_URI", photoFileName)
            fragment.arguments = args
            return fragment
        }
    }
}
