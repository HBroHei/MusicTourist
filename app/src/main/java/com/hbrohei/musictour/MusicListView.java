package com.hbrohei.musictour;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MusicListView extends RecyclerView.Adapter<MusicListView.ViewHolder> {

    private String[] musicFileList;

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageButton playBtn;
        private final ImageButton menuBtn;
        private TextView itemName;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            itemName = view.findViewById(R.id.itemName);
            playBtn = view.findViewById(R.id.playBtnMenu);
            menuBtn = view.findViewById(R.id.menuBtn);

            playBtn.setOnClickListener(v -> {

            });
            menuBtn.setOnClickListener(v ->{
                PopupMenu menu = new PopupMenu(view.getContext(),menuBtn);
                menu.getMenuInflater().inflate(R.menu.song_sel_menu,menu.getMenu());
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if(item.getItemId()==R.id.del){
                            AlertDialog.Builder delDialog = new AlertDialog.Builder(view.getContext());
                            delDialog.setTitle("Delete this item?");
                            delDialog.setMessage("This cannot be undo.");
                            delDialog.setNegativeButton("Cancel",null);
                            delDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) { //TODO make it work
                                    //final String textname = itemName.getText().toString();

                                    //Log.d("DELETE_FILE",textname);

                                    int idxRemoved = -1;
                                    String[] currentMusicFilesList = new File(view.getContext().getExternalFilesDir(null) + "/Custom Music").list((dir, name) -> name.toLowerCase(Locale.ROOT).contains(".mp3"));
                                    ArrayList<String> newMusicFileList = new ArrayList<>();
                                    for(int i=0;i<musicFileList.length;i++){
                                        //Check if the item description is the same
                                        if(!musicFileList[i].equals(itemName.getText().toString())){
                                            newMusicFileList.add(musicFileList[i]);
                                        }
                                        else{
                                            //Mark down the index
                                            idxRemoved = i;
                                        }
                                    }

                                    final String textname = currentMusicFilesList[idxRemoved];

                                    SharedPreferences.Editor loopTimeSP = view.getContext().getSharedPreferences("MTour_duration",Context.MODE_PRIVATE).edit();
                                    loopTimeSP.remove(textname);
                                    loopTimeSP.apply();

                                    SharedPreferences.Editor songNameSP = view.getContext().getSharedPreferences("MTour_name",Context.MODE_PRIVATE).edit();
                                    songNameSP.remove(textname);
                                    songNameSP.apply();

                                    musicFileList = newMusicFileList.toArray(new String[0]);

                                    if(idxRemoved!=-1){
                                        //String[] currentMusicFilesList = new File(view.getContext().getExternalFilesDir(null) + "/Custom Music").list((dir, name) -> name.toLowerCase(Locale.ROOT).contains(".mp3"));
                                        if(!new File(view.getContext().getExternalFilesDir(null) + "/Custom Music/" + currentMusicFilesList[idxRemoved]).delete()){
                                            Toast.makeText(view.getContext(), "Error: Cannot delete file", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        notifyItemRemoved(idxRemoved);
                                    }
                                    Toast.makeText(view.getContext(), "Removed", Toast.LENGTH_SHORT).show();
                                }
                            });
                            delDialog.show();
                        }
                        return false;
                    }
                });
                menu.show();
            });
        }

        public TextView getTextView() {
            return itemName;
        }
    }

    public MusicListView(String[] dataSet) {
        musicFileList = dataSet;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.song_list_element, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        //Set element with content at position
        viewHolder.getTextView().setText(musicFileList[position]);


    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return musicFileList.length;
    }
}
