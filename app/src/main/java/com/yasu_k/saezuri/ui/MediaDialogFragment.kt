package com.yasu_k.saezuri.ui

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.yasu_k.saezuri.MediaOptions
import com.yasu_k.saezuri.R

enum class Option {
    SELECT_IMAGES, SELECT_ONE_VIDEO, TAKE_ONE_PHOTO, CAPTURE_ONE_VIDEO, NOT_CHOSEN
}

class MediaDialogFragment : DialogFragment() {

    companion object {
        fun newInstance() = MediaDialogFragment()
    }

    //private lateinit var viewModel: DialogViewModel
    private var chosenOption = Option.NOT_CHOSEN

    // Use this instance of the interface to deliver action events
    private lateinit var listener: NoticeDialogListener

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    interface NoticeDialogListener {
        fun onOptionClick(whichOption: Option)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            //val builder = MaterialAlertDialogBuilder(it)
            val dialogStartMessage = "Start a dialog"
            builder.setTitle(dialogStartMessage)
                .setItems(
                    MediaOptions.mediaOptions
                ) { dialogInterface: DialogInterface?, whichOption: Int ->
                    chosenOption = when (whichOption) {
                        MediaOptions.SELECT_IMAGES -> Option.SELECT_IMAGES
                        MediaOptions.SELECT_A_VIDEO -> Option.SELECT_ONE_VIDEO
                        MediaOptions.TAKE_A_PHOTO -> Option.TAKE_ONE_PHOTO
                        MediaOptions.CAPTURE_A_VIDEO -> Option.CAPTURE_ONE_VIDEO
                        else -> Option.NOT_CHOSEN
                    }
                }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        //To send "chosenOption" back to the parent fragment
        listener.onOptionClick(chosenOption)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dialog, container, false)
    }

//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)
//        viewModel = ViewModelProvider(this).get(DialogViewModel::class.java)
//        // TODO: Use the ViewModel
//    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            val fragment = parentFragment
            //Log.i(TAG, "parentFragment = $parentFragment")
            listener = fragment as NoticeDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException((context.toString() +
                    " must implement NoticeDialogListener. Exception: $e"))
        }
    }
}