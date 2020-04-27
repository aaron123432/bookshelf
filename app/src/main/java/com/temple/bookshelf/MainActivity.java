package com.temple.bookshelf;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.AndroidAuthenticator;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import edu.temple.audiobookplayer.AudiobookService;
import edu.temple.audiobookplayer.AudiobookService.MediaControlBinder;

public class MainActivity extends AppCompatActivity implements BookListFragment.BookSelectedInterface, BookDetailsFragment.audio_control {

    private static final String BOOKS_KEY = "books";
    private static final String SELECTED_BOOK_KEY = "selectedBook";


    FragmentManager fm;

    boolean twoPane;
    BookListFragment bookListFragment;
    BookDetailsFragment bookDetailsFragment;

    ArrayList<Book> books;
    RequestQueue requestQueue;
    Book selectedBook;

    EditText searchEditText;
    private final String SEARCH_API = "https://kamorris.com/lab/abp/booksearch.php?search=";


    // audiobook connection
    AudiobookService.MediaControlBinder binder;
    boolean isConnect = false;
    Intent service_intent;
    Handler handler;
    final static String playing_text = "current playing : ";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save previously searched books as well as selected book
        outState.putParcelableArrayList(BOOKS_KEY, books);
        outState.putParcelable(SELECTED_BOOK_KEY, selectedBook);
    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (AudiobookService.MediaControlBinder) service;
            binder.setProgressHandler(handler);
            System.out.println("connection success");
            isConnect = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            System.out.println("connection fail");
            isConnect = false;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        service_intent = new Intent(this, AudiobookService.class);
        startService(service_intent);
        bindService(service_intent, serviceConnection, Context.BIND_AUTO_CREATE);
        seekbar();



        /*
        Perform a search
         */
        searchEditText = findViewById(R.id.searchEditText);
        findViewById(R.id.searchButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchBooks(searchEditText.getText().toString());
            }
        });

        /*
        If we previously saved a book search and/or selected a book, then use that
        information to set up the necessary instance variables
         */
        if (savedInstanceState != null) {
            books = savedInstanceState.getParcelableArrayList(BOOKS_KEY);
            selectedBook = savedInstanceState.getParcelable(SELECTED_BOOK_KEY);
        } else
            books = new ArrayList<Book>();

        twoPane = findViewById(R.id.container2) != null;
        fm = getSupportFragmentManager();

        requestQueue = Volley.newRequestQueue(this);

        /*
        Get an instance of BookListFragment with an empty list of books
        if we didn't previously do a search, or use the previous list of
        books if we had previously performed a search
         */
        bookListFragment = BookListFragment.newInstance(books);

        fm.beginTransaction()
                .replace(R.id.container1, bookListFragment)
                .commit();

        /*
        If we have two containers available, load a single instance
        of BookDetailsFragment to display all selected books.

        If a book was previously selected, show that book in the book details fragment
        *NOTE* we could have simplified this to a single line by having the
        fragment's newInstance() method ignore a null reference, but this way allow
        us to limit the amount of things we have to change in the Fragment's implementation.
         */
        if (twoPane) {
            if (selectedBook != null) {
                bookDetailsFragment = BookDetailsFragment.newInstance(selectedBook);

            } else {
                bookDetailsFragment = new BookDetailsFragment();
            }

            fm.beginTransaction()
                    .replace(R.id.container2, bookDetailsFragment)
                    .commit();
        } else {
            if (selectedBook != null) {
                fm.beginTransaction()
                        .replace(R.id.container1, BookDetailsFragment.newInstance(selectedBook))
                        // Transaction is reversible
                        .addToBackStack(null)
                        .commit();
            }
        }
    }

    /*
    Fetch a set of "books" from from the web service API
     */

    private void fetchBooks(String searchString) {
        /*
        A Volloy JSONArrayRequest will automatically convert a JSON Array response from
        a web server to an Android JSONArray object
         */
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(SEARCH_API + searchString, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                if (response.length() > 0) {
                    books.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject bookJSON;
                            bookJSON = response.getJSONObject(i);
                            books.add(new Book(bookJSON.getInt(Book.JSON_ID),
                                    bookJSON.getString(Book.JSON_TITLE),
                                    bookJSON.getString(Book.JSON_AUTHOR),
                                    bookJSON.getString(Book.JSON_COVER_URL), bookJSON.getInt(Book.JSON_DURATION)));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    updateBooksDisplay();
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.search_error_message), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        requestQueue.add(jsonArrayRequest);
    }

    ;

    private void updateBooksDisplay() {
        /*
        Remove the BookDetailsFragment from the container after a search
        if it is the currently attached fragment
         */
        if (fm.findFragmentById(R.id.container1) instanceof BookDetailsFragment)
            fm.popBackStack();
        bookListFragment.updateBooksDisplay(books);
    }

    @Override
    public void bookSelected(int index) {
        selectedBook = books.get(index);
        if (twoPane) {
            /*
            Display selected book using previously attached fragment
             */
            bookDetailsFragment.displayBook(selectedBook);
            bookDetailsFragment.book = selectedBook;
        } else {
            /*
            Display book using new fragment
             */
            fm.beginTransaction()
                    .replace(R.id.container1, BookDetailsFragment.newInstance(selectedBook))
                    // Transaction is reversible
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void play(int i, Book book) {
        binder.play(i);
    }

    @Override
    public void pause() {
        binder.pause();
    }

    @Override
    public void stop() {
        binder.stop();
    }

    @Override
    public void seekbar() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                AudiobookService.BookProgress bookProgress = (AudiobookService.BookProgress) msg.obj;
                if (bookProgress == null) {
                }
                else {
                    Book current_book  = getbook_byID(bookProgress.getBookId());
                    System.out.println("the current book is " + current_book);
                    double duration = current_book.getDuration() * 1.0;
                    double haven_play = bookProgress.getProgress() * 1.0;
                    double progress = (haven_play/duration) * 100;
                    System.out.println(progress);
                    int progress_int = (int) progress;

                    SeekBar seekBar = findViewById(R.id.music_progressBar);
                    TextView textView = findViewById(R.id.current_playing);
                    if(seekBar == null){
                        System.out.println("nothing is there");
                    }else{
                        System.out.println("this is not a null object");
                        seekBar.setProgress(progress_int);
                        textView.setText(playing_text + current_book.getTitle());
                    }

                }
            }
        };

    }

    @Override
    public void seekbar_change(int progress, Book book) {


        double d = progress/100.0;
        double current_real = d * book.getDuration();
        int current_progress = (int) current_real;
        binder.play(book.getId(),current_progress);

        System.out.println("----------seekbar_change----------");
        System.out.println("the current progress: " + progress + "%");
        System.out.println("book duration : " + book.getDuration());
        System.out.println("the actual progress in seekbar_change: " + current_progress);
        System.out.println("----------seekbar_change----------");

    }

    public Book getbook_byID(int book_id){
        for(Book book : books){
            if(book.getId() == book_id){
                return book;
            }
        }
        return null;
    }



}

