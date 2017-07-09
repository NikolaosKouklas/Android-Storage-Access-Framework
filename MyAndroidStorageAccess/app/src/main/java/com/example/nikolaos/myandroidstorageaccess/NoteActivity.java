package com.example.nikolaos.myandroidstorageaccess;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jensdriller.libs.undobar.UndoBar;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class NoteActivity extends AppCompatActivity implements UndoBar.Listener {

    // Request Codes are passed on to the onActivityResult() method
    // through the intents enabling the method to identify which action has been requested by the user.
    private static final int CREATE_REQUEST_CODE = 40;
    private static final int OPEN_REQUEST_CODE = 41;
    private static final int SAVE_REQUEST_CODE = 42;

    // The states of the app that help to handle when the saveButton is enabled or disabled accordingly
    // More specifically, the saveButton is disabled when there is no need for the save operation
    private static final int NEW_NOTE_STATE = 30;
    private static final int OPENED_NOTE_STATE = 31;
    private static final int CLOSED_NOTE_STATE = 32;

    private TextView fileSelectedTextView; // here is displayed the name of file that has been selected
    private EditText noteEditText; // here is entered or modified the text of a Note

    // The 3 buttons of UI
    private Button newButton;
    private Button openButton;
    private Button saveButton;

    private Uri currentUri = null; // No file has been selected
    private int noteState = CLOSED_NOTE_STATE; // The state of the app is related with a closed Note.
    private String temp = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        fileSelectedTextView = (TextView) findViewById(R.id.fileTextView);
        noteEditText = (EditText) findViewById(R.id.noteEditText);
        newButton = (Button) findViewById(R.id.newButton);
        openButton = (Button) findViewById(R.id.openButton);
        saveButton = (Button) findViewById(R.id.saveButton);

        saveButton.setEnabled(false);

        // Here i attach an addTextChangedListener to noteEditText view so as to handle the saveButton
        // when the text in noteEditText is changing.
        // http://stackoverflow.com/questions/22680106/how-to-disable-button-if-edittext-is-empty
        noteEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                // When my app is on state NEW_NOTE_STATE or CLOSED_NOTE_STATE and
                // there is no text in the noteEditText view, then disable the saveButton. Otherwise, enable it.
                if(noteState == NEW_NOTE_STATE || noteState == CLOSED_NOTE_STATE) {

                    if (s.toString().trim().length() == 0) {
                        saveButton.setEnabled(false);
                    } else {
                        saveButton.setEnabled(true);
                    }
                }
                // When my app is on state OPENED_NOTE_STATE, enable the saveButton when the current text in the noteEditText view
                // is different from the initial text that was loaded after the openButton was clicked. Otherwise disable it.
                else if(noteState == OPENED_NOTE_STATE) {

                    if(s.toString().equals(temp)) {
                        saveButton.setEnabled(false);
                    }
                    else {
                        saveButton.setEnabled(true);
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }
        });
    }

    // This function is called when the newButton is clicked, for the creation
    // of a new text file on a local or cloud storage provider through an internal UI storage picker
    // http://www.techotopia.com/index.php/An_Android_Storage_Access_Framework_Example
    public void newTextFile(View view)
    {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "newNote.txt");

        startActivityForResult(intent, CREATE_REQUEST_CODE);
    }

    // This function is called when the saveButton is clicked
    // In the case that no file has been selected, we can select where to store a note via the internal UI storage picker
    // Otherwise, if a file has been selected we can store directly a note without the internal UI storage picker via the currentUri
    // http://www.techotopia.com/index.php/An_Android_Storage_Access_Framework_Example
    public void saveTextFile(View view)
    {
        // no file has been selected
        if(currentUri == null) {

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            startActivityForResult(intent, SAVE_REQUEST_CODE);
        }
        // a file has been selected
        else {

            try {
                // write the text or note contained in noteEditText to a file via a uri
                writeFileContent(currentUri);
                Toast.makeText(this, "Note saved successfully!", Toast.LENGTH_LONG).show();
            }
            catch (Exception e) {

                Toast.makeText(this, "Note failed to be saved!"+" File "+fileSelectedTextView.getText().toString()+" not found!", Toast.LENGTH_LONG).show();
                closeCurrentNote();
                return;
            }

            // This is necessary in the case that another app has renamed this file.
            fileSelectedTextView.setText(getFileNameFromUri(currentUri));
            // this is needed when we save a new text file so as to go from NEW_NOTE_STATE to OPENED_NOTE_STATE
            noteState = OPENED_NOTE_STATE;
            // we store temporarily the text contained in noteEditText to a temp String variable
            temp = noteEditText.getText().toString();
            // disable saveButton after the save operation has been completed
            saveButton.setEnabled(false);
        }

    }

    // This function is called when the openButton is clicked, to open
    // a text file from a local or cloud storage provider through an internal UI storage picker
    // http://www.techotopia.com/index.php/An_Android_Storage_Access_Framework_Example
    public void openTextFile(View view)
    {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, OPEN_REQUEST_CODE);
    }

    // http://www.techotopia.com/index.php/An_Android_Storage_Access_Framework_Example
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        if (resultCode == Activity.RESULT_OK)
        {
            // the action that must be performed from NoteActivity when UI storage picker returned CREATE_REQUEST_CODE request
            if (requestCode == CREATE_REQUEST_CODE)
            {
                if (resultData != null) {

                    currentUri = resultData.getData();

                    fileSelectedTextView.setText(getFileNameFromUri(currentUri));
                    noteEditText.setText("");
                    noteState = NEW_NOTE_STATE;
                    saveButton.setEnabled(false); // there is no need to save an empty new file again as it has already been created
                    Toast.makeText(this, "Empty Note creation was successful!", Toast.LENGTH_LONG).show();
                }
                else
                {
                    Toast.makeText(this, "Empty Note creation failed!", Toast.LENGTH_LONG).show();
                }
            }
            // the action that must be performed from NoteActivity when UI storage picker returned SAVE_REQUEST_CODE request
            else if (requestCode == SAVE_REQUEST_CODE) {

                if (resultData != null) {

                    Uri tempUri = resultData.getData();

                    try {
                        writeFileContent(tempUri);
                        Toast.makeText(this, "Note saved in "+getFileNameFromUri(tempUri)+" successfully!", Toast.LENGTH_LONG).show();
                    }
                    catch (Exception e) {

                        Toast.makeText(this, "Note failed to save in "+getFileNameFromUri(tempUri), Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }
            // the action that must be performed from NoteActivity when UI storage picker returned OPEN_REQUEST_CODE request
            else if (requestCode == OPEN_REQUEST_CODE)
            {
                if (resultData != null) {

                    currentUri = resultData.getData();
                    String content, filename;
                    content = filename = "";

                    try {
                        filename = getFileNameFromUri(currentUri);
                        content = readFileContent(currentUri);
                        noteEditText.setText(content);
                        temp = noteEditText.getText().toString();
                        fileSelectedTextView.setText(filename);
                        noteState = OPENED_NOTE_STATE;
                        saveButton.setEnabled(false);
                    }
                    catch (IOException e) {
                        Toast.makeText(this, "File "+filename+" failed to be opened!", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }
        }
    }

    // http://www.techotopia.com/index.php/An_Android_Storage_Access_Framework_Example
    private String readFileContent(Uri uri) throws IOException {

        InputStream inputStream = getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder stringBuilder = new StringBuilder();
        String currentline;

        while ((currentline = reader.readLine()) != null) {
            stringBuilder.append(currentline + "\n");
        }

        inputStream.close();
        return stringBuilder.toString();
    }

    // http://www.techotopia.com/index.php/An_Android_Storage_Access_Framework_Example
    private void writeFileContent(Uri uri)
    {
        try {
            ParcelFileDescriptor pfd = this.getContentResolver().openFileDescriptor(uri, "w");

            FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());

            String textContent = noteEditText.getText().toString();

            fileOutputStream.write(textContent.getBytes());

            fileOutputStream.close();
            pfd.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // With this function we get the name of file from a uri
    // https://developer.android.com/guide/topics/providers/document-provider.html
    // http://www.techotopia.com/index.php/An_Android_Storage_Access_Framework_Example
    private String getFileNameFromUri(Uri uri) {

        String displayName = null;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null, null);

        try {

            if (cursor != null && cursor.moveToFirst()) {

                displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        }
        finally {
            cursor.close();
        }

        return displayName;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_clear:
                clearCurrentNote();
                return true;
            case R.id.action_close:
                closeCurrentNote();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // this function is called when we click Close Note from menu
    private void closeCurrentNote() {

        noteEditText.setText("");
        fileSelectedTextView.setText("");
        currentUri = null;
        noteState = CLOSED_NOTE_STATE;
        saveButton.setEnabled(false);
    }

    // this function is called when we click Clear Note from menu
    private void clearCurrentNote() {

        temp = noteEditText.getText().toString();
        noteEditText.setText("");

        // https://github.com/jenzz/Android-UndoBar
        new UndoBar.Builder(this)
                .setMessage("Current Note cleared!")
                .setListener(this)
                .setStyle(UndoBar.Style.HOLO)
                .show();
    }

    // required for undo operation
    @Override
    public void onHide() {

        temp = ""; // after the undo bar was hidden clear temp variable
    }

    // required for undo operation
    @Override
    public void onUndo(Parcelable parcelable) {

        noteEditText.setText(temp); // when undo bar is clicked copy text from temp String back to noteEditText view
    }
}
