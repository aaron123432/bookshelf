package com.temple.bookshelf;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.HashMap;


public class BookDetailsFragment extends Fragment {

    private static final String BOOK_KEY = "book";
    public Book book;

    TextView titleTextView, authorTextView;
    ImageView coverImageView;


    //parent
    audio_control parent;
    int book_id;
    //Buttons
    Button play_button;
    Button pause_button;
    Button stop_button;
    SeekBar seekBar;

    public BookDetailsFragment() {
    }

    public static BookDetailsFragment newInstance(Book book) {
        BookDetailsFragment fragment = new BookDetailsFragment();
        Bundle args = new Bundle();

        /*
         Our Book class implements the Parcelable interface
         therefore we can place one inside a bundle
         by using that put() method.
         */
        args.putParcelable(BOOK_KEY, book);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            book = (Book) getArguments().getParcelable(BOOK_KEY);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof audio_control) {
            parent = (audio_control) context;
        } else {
            throw new RuntimeException("Please implement the required interface(s)");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment, container, false);

        titleTextView = v.findViewById(R.id.titleTextView);
        authorTextView = v.findViewById(R.id.authorTextView);
        coverImageView = v.findViewById(R.id.coverImageView);

        //button setup
        play_button = v.findViewById(R.id.play_button);
        pause_button = v.findViewById(R.id.pause_button);
        stop_button = v.findViewById(R.id.stop__button);






        play_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.stop();
                parent.play(book.getId(),book);
            }
        });

        pause_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.pause();
            }
        });

        stop_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.stop();
            }
        });


        //seekbar_setup
        seekBar = v.findViewById(R.id.music_progressBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                parent.stop();
                parent.seekbar_change(seekBar.getProgress(), book);
            }
        });


        /*
        Because this fragment can be created with or without
        a book to display when attached, we need to make sure
        we don't try to display a book if one isn't provided
         */
        if (book != null)
            displayBook(book);
        return v;
    }

    /*
    This method is used both internally and externally (from the activity)
    to display a book
     */
    public void displayBook(Book book) {
        titleTextView.setText(book.getTitle());
        authorTextView.setText(book.getAuthor());
        // Picasso simplifies image loading from the web.
        // No need to download separately.
        Picasso.get().load(book.getCoverUrl()).into(coverImageView);
    }

    interface audio_control {
        void play(int i,Book book);
        void pause();
        void stop();
        void seekbar();
        void seekbar_change(int progress,Book book);

    }

}
