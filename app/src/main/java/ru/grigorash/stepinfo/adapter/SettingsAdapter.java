package ru.grigorash.stepinfo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;


import java.util.ArrayList;

import ru.grigorash.stepinfo.R;

public class SettingsAdapter extends BaseAdapter
{
    private static final int TYPE_SIMPLE_SETTING = 1;

    public static class SimpleSetting
    {
        private String m_title;
        private String m_summary;
        private String m_key;

        public SimpleSetting(String title, String summary, String key)
        {
            m_title = title;
            m_summary = summary;
            m_key = key;
        }

        public String getKey()
        {
            return m_key;
        }

        public String getTitle()
        {
            return  m_title;
        }

        public void setTitle(String title)
        {
            m_title = title;
        }

        public String getSummary()
        {
            return m_summary;
        }

        public void setSummary(String summary)
        {
            m_summary = summary;
        }
    }

    private class SimpleSettingHolder
    {
        private TextView m_tv_title;
        private TextView m_tv_summary;

        public SimpleSettingHolder(@NonNull View itemView)
        {
            m_tv_title = itemView.findViewById(R.id.setting_name);
            m_tv_summary = itemView.findViewById(R.id.setting_summary);
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

        public void setData(SimpleSetting setting)
        {
            if (m_tv_title != null)
                m_tv_title.setText(setting.getTitle());
            if (m_tv_summary != null)
                m_tv_summary.setText(setting.getSummary());
        }
    }

    private Context m_context;
    private ArrayList<SimpleSetting> m_settings;

    public SettingsAdapter(Context context, ArrayList<SimpleSetting> settings)
    {
        m_context = context;
        m_settings = settings;
    }

    @Override
    public int getCount()
    {
        return m_settings.size();
    }

    @Override
    public Object getItem(int position)
    {
        return m_settings.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // for multiple item views
        // int listViewItemType = getItemViewType(position);
        SimpleSettingHolder viewHolder;
        if (convertView == null)
        {
            convertView = LayoutInflater.from(m_context).inflate(R.layout.item_simple_setting, parent, false);
            viewHolder = new SimpleSettingHolder(convertView);
            convertView.setTag(viewHolder);
        }
        else
            viewHolder = (SimpleSettingHolder)convertView.getTag();
        viewHolder.setData(m_settings.get(position));
        return convertView;
    }

    @Override
    public int getItemViewType(int position)
    {
        return TYPE_SIMPLE_SETTING;
    }
}
