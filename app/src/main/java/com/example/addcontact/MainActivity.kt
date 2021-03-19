package com.example.addcontact

import android.app.Activity
import android.content.ContentProviderOperation
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.Image
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.addcontact.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "CONTACT_ADD_TAG"
    private lateinit var contactPermission: Array<String>
    private val WRITE_CONTACT_PERMISSION_DONE = 100
    private val IMAGE_PICK_GALLERY_CODE = 200
    private var image_uri:Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactPermission = arrayOf(android.Manifest.permission.WRITE_CONTACTS)
        binding.profileIv.setOnClickListener {
            openGalleryIntent()

        }

        binding.saveFab.setOnClickListener {
            if (isWriteContactPermissionEnable()) {
                saveContact()
            } else {
                requestWriteContactPermission()
            }
        }
    }

    private fun saveContact() {
        Log.d(TAG, "saveContact: ")

        val firstName = binding.firstNameEt.text.toString().trim()
        val lastName = binding.lastNameEt.text.toString().trim()
        val phoneMobile = binding.phoneMobileEt.text.toString().trim()
        val phoneHome = binding.phoneHomeEt.text.toString().trim()
        val email = binding.emailEt.text.toString().trim()
        val address = binding.addressEt.text.toString().trim()

        Log.d(TAG, "saveContact: First Name $firstName")
        Log.d(TAG, "saveContact: Last Name $lastName")
        Log.d(TAG, "saveContact: Phone Mobile $phoneHome")
        Log.d(TAG, "saveContact: Phone Home $phoneHome")
        Log.d(TAG, "saveContact: Email $email")
        Log.d(TAG, "saveContact: Address $address")

        val cpo = ArrayList<ContentProviderOperation>()
        val rawContactId = cpo.size
        cpo.add(ContentProviderOperation.newInsert(
            ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            .build())

        cpo.add(ContentProviderOperation.newInsert(
            ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
            .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
            .build())

        cpo.add(ContentProviderOperation.newInsert(
            ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
            .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneMobile)
            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
            .build())

        cpo.add(ContentProviderOperation.newInsert(
            ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
            .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneHome)
            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_HOME)
            .build())

        cpo.add(ContentProviderOperation.newInsert(
            ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
            .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Email.DATA, email)
            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
            .build())

        cpo.add(ContentProviderOperation.newInsert(
            ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
            .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.StructuredPostal.DATA, address)
            .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
            .build())

        val imageBytes = imageUriToBytes()
        if (imageBytes != null) {
            Log.d(TAG, "saveContact: contact with image")
            cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, imageBytes).build())
        }
        else {
            Log.d(TAG, "saveContact: contact without image")
        }
        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, cpo)
            Log.d(TAG,"saveContact: Saved")
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception) {
            Log.d(TAG, "saveContact: failed to save due to ${e.message}")
            Toast.makeText(this, "failed to save due to ${e.message}", Toast.LENGTH_SHORT).show()
        }

    }

    private fun imageUriToBytes(): ByteArray? {
        val bitmap: Bitmap
        val baos: ByteArrayOutputStream?

        return try {
            if (Build.VERSION.SDK_INT < 28) {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, image_uri)
            }
            else {
                val source = ImageDecoder.createSource(contentResolver, image_uri!!)
                bitmap = ImageDecoder.decodeBitmap(source)
            }

            baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
            baos.toByteArray()
        } catch (e: Exception) {
            Log.d(TAG, "imageUriToBytes: ${e.message}")
            null
        }
    }

    private fun isWriteContactPermissionEnable(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestWriteContactPermission() {
        ActivityCompat.requestPermissions(this, contactPermission, WRITE_CONTACT_PERMISSION_DONE)
    }

    private fun openGalleryIntent() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            if (requestCode == WRITE_CONTACT_PERMISSION_DONE) {
                val haveWriteContactPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (haveWriteContactPermission) {
                    saveContact()
                }
                else {
                    Toast.makeText(this, "Permission dened", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                image_uri = data!!.data
                binding.profileIv.setImageURI(image_uri)
            }
        }
        else {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()

        }
    }
}