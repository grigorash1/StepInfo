package ru.grigorash.stepinfo.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.balsikandar.crashreporter.CrashReporter;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import ru.grigorash.stepinfo.R;
import ru.grigorash.stepinfo.track.TrackInfo;

public class TracksAdapter extends BaseAdapter
{
    private static DecimalFormat df2 = new DecimalFormat("#.##");

    private class TrackViewHolder
    {
        private Activity m_parent;
        private TextView m_tv_name;
        private TextView m_tv_length;
        private TextView m_tv_avg_speed;

        public TrackViewHolder(@NotNull Activity parent, @NonNull View itemView)
        {
            m_parent = parent;
            m_tv_name = itemView.findViewById(R.id.track_name);
            m_tv_name.setText("⌛");
            m_tv_length = itemView.findViewById(R.id.total_length);
            m_tv_length.setText("⌛");
            m_tv_avg_speed = itemView.findViewById(R.id.avg_speed);
            m_tv_avg_speed.setText("⌛");

            /*
            itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    int i = 0;
                }
            });*/
        }

        private void setTrack(@NonNull TrackInfo track)
        {
            m_tv_name.setText(track.name());
            track.asyncGetInfo(result -> setData(result));
        }

        private void setData(@NonNull TrackInfo complete_info)
        {
            m_parent.runOnUiThread(() ->
            {
                if (m_tv_length != null)
                    m_tv_length.setText(df2.format(complete_info.length()) + " km");
                if (m_tv_avg_speed != null)
                    m_tv_avg_speed.setText(df2.format(complete_info.avgSpeed()) + " km/h");
            });
        }
    }

    private Activity m_parent;
    private List<TrackInfo> m_tracks;

    public TracksAdapter(@NotNull Activity parent, @NotNull File base_dir)
    {
        try
        {
            m_parent = parent;
            m_tracks = new ArrayList<>();
            String[] track_files = base_dir.list((dir, name) -> name.endsWith(".bin"));
            for (int i = 0; i < track_files.length; i++)
                m_tracks.add(new TrackInfo(base_dir.getAbsolutePath() + "/" + track_files[i]));
        }
        catch (Exception e)
        {
            CrashReporter.logException(e);
        }
    }

    @Override
    public int getCount() { return m_tracks.size(); }

    @Override
    public Object getItem(int position)  { return m_tracks.get(position); }

    @Override
    public long getItemId(int position)  { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        TrackViewHolder viewHolder;
        if (convertView == null)
        {
            convertView = LayoutInflater.from(m_parent).inflate(R.layout.item_track, parent, false);
            viewHolder = new TrackViewHolder(m_parent, convertView);
            convertView.setTag(viewHolder);
        }
        else
            viewHolder = (TrackViewHolder)convertView.getTag();
        viewHolder.setTrack(m_tracks.get(position));
        return convertView;

    }
}
