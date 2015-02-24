package com.aus.sgsp;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static com.aus.sgsp.Api.*;


public class EventAdapter extends BaseAdapter {

    private final Context context;
    private List<Page.Event> events;


    // Multiple comparators selected for use from a menu would allow for easy and interchangeable
    // sorting
    private Comparator<Page.Event> localTimeComp = (lhs, rhs) -> {

        // TODO:  Bad error handling.  Malformed data should be caught when it comes in...
        try {
            Date ld = Util.parseTimeStr(lhs.datetime_local);
            Date rd = Util.parseTimeStr(rhs.datetime_local);
            return ld.compareTo(rd);
        } catch (ParseException e) {
            e.printStackTrace();
            // If it breaks, nobody wins
            return 0;
        }
    };

    public void localTimeSort() {
        keepSorted(localTimeComp);
        notifyDataSetChanged();
    }

    private void keepSorted(Comparator<Page.Event> c) {
        Collections.sort(events, c);
    }

    public EventAdapter(Context ctx) {
        context = ctx;
        events = new ArrayList<>();
    }

    public EventAdapter(Context ctx, List<Page.Event> evts) {
        context = ctx;
        events = evts;
    }

    public void addEvent(Page.Event event) {
        // TODO:  more sophisticated equivalence checking
        events.add(event);
        notifyDataSetChanged();

    }

    @Override
    public int getCount() {
        return events.size();
    }

    @Override
    public Object getItem(int position) {
        return events.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        ImageView image;
        TextView title;
        TextView date;
        TextView price;
        TextView score;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if(convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item, null);

            holder = new ViewHolder();
            holder.image = (ImageView) convertView.findViewById(R.id.image);
            holder.title = (TextView) convertView.findViewById(R.id.title);
            holder.date = (TextView) convertView.findViewById(R.id.date);
            holder.price = (TextView) convertView.findViewById(R.id.price);
            holder.score = (TextView) convertView.findViewById(R.id.score);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Page.Event event = events.get(position);

        holder.title.setSelected(true);  // Set focus to the textview
        holder.title.setText(event.short_title);
        holder.title.setSelected(true);
        holder.title.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        holder.title.setSingleLine(true);

        try {
            DateFormat df = DateFormat.getDateTimeInstance();
            String time = df.format(Util.parseTimeStr(event.datetime_local));
            holder.date.setText(time);
        } catch (ParseException e) {
            e.printStackTrace();
            holder.date.setText(event.datetime_local);
        }

        if(event.stats.get("lowest_price") == null || event.stats.get("highest_price") == null) {
            holder.price.setVisibility(View.INVISIBLE);
        } else {
            holder.price.setVisibility(View.VISIBLE);
            String lowHigh = "Low : $" + event.stats.get("lowest_price");
            lowHigh += " | High : $" + event.stats.get("highest_price");
            holder.price.setText(lowHigh);
        }

        holder.score.setText("Score : " + event.score);

        String url = "";

        if(!event.performers.isEmpty()) {
            // Performers are sorted by rating anyway
            Page.Event.Performer perf = event.performers.get(0);
            if(perf.images.containsKey("huge")) {
                url = perf.images.get("huge");

            } else if(perf.images.containsKey("large"))
            {
                url = perf.images.get("large");
            } else if(perf.images.containsKey("medium"))
            {
                url = perf.images.get("medium");
            } else if(perf.images.containsKey("small"))
            {
                url = perf.images.get("small");
            }

            if(!url.equals("")) {
                Picasso.with(context)
                        .load(url)
                        .placeholder(R.mipmap.ic_launcher)
                        .into(holder.image);
            } else {
                Picasso.with(context)
                        .load(R.mipmap.ic_launcher)
                        .rotate(90)
                        .into(holder.image);
            }
        }

        return convertView;
    }

}
