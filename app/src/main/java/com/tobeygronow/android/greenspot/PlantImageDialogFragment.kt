package com.tobeygronow.android.greenspot

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment


class PlantImageDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.zoomed_image_dialog_fragment, container, false)
        val imageView = view.findViewById(R.id.zoomed_image_view) as ImageView

        val photoFileName = arguments?.getSerializable("PHOTO_URI") as String
        imageView.setImageBitmap(BitmapFactory.decodeFile(requireContext().filesDir.path + "/" + photoFileName))

        return view
    }

    companion object {
        fun newInstance(photoFileName: String): PlantImageDialogFragment {
            val frag = PlantImageDialogFragment()
            val args = Bundle()
            args.putSerializable("PHOTO_URI", photoFileName)
            frag.arguments = args
            return frag
        }
    }
}
