package com.sourav.weatherfore;


import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.sourav.weatherfore.db.WeatherContract;
import com.sourav.weatherfore.utilities.WeatherDateUtils;
import com.sourav.weatherfore.utilities.WeatherUtils;

/**
 * Created by Sourav on 3/16/2018.
 */

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>{

    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;

    // Flag to determine if we want to use a separate view for "today".
    private boolean mUseTodayLayout = true;

    private Cursor mCursor;
    final private Context mContext;
    final private ForecastAdapterOnClickHandler mClickHandler;
    final private View mEmptyView;
    final private ItemChoiceManager mICM;


    @Override
    public ForecastViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewGroup instanceof RecyclerView) {
            int layoutId = 0;
            switch (viewType) {
                case VIEW_TYPE_TODAY: {
                    layoutId = R.layout.today_forecast_list_item;
                    break;
                }
                case VIEW_TYPE_FUTURE_DAY: {
                    layoutId = R.layout.forecast_list_item;
                    break;
                }
            }
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(layoutId, viewGroup, false);
            view.setFocusable(true);
            return new ForecastViewHolder(view);
        } else {
            throw new RuntimeException("Not bound to RecyclerView");
        }
    }


    @Override
    public void onBindViewHolder(ForecastViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        int weatherId = mCursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        int defaultImage;

        switch (getItemViewType(position)) {
            case VIEW_TYPE_TODAY:
                defaultImage = WeatherUtils.getLargeArtResourceIdForWeatherCondition(weatherId);
                break;
            default:
                defaultImage = WeatherUtils.getSmallArtResourceIdForWeatherCondition(weatherId);
        }

            Glide.with(mContext)
                    .load(WeatherUtils.getArtUrlForWeatherCondition(weatherId))
                    .error(defaultImage)
                    .crossFade()
                    .into(holder.mIconView);

        // this enables better animations. even if we lose state due to a device rotation,
        // the animator can use this to re-find the original view
        ViewCompat.setTransitionName(holder.mIconView, "iconView" + position);

        // Read date from cursor
        long dateInMillis = mCursor.getLong(ForecastFragment.COL_WEATHER_DATE);

        // Find TextView and set formatted date on it
        holder.mDateView.setText(WeatherDateUtils.getFriendlyDateString(mContext, dateInMillis));

        // Read weather forecast from cursor
        String description = WeatherUtils.getStringForWeatherCondition(mContext, weatherId);

        // Find TextView and set weather forecast on it
        holder.mDescriptionView.setText(description);
        holder.mDescriptionView.setContentDescription(mContext.getString(R.string.a11y_forecast, description));

        // For accessibility, we don't want a content description for the icon field
        // because the information is repeated in the description view and the icon
        // is not individually selectable

        // Read high temperature from cursor
        double high = mCursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
        String highString = WeatherUtils.formatTemperature(mContext, high);
        holder.mHighTempView.setText(highString);
        holder.mHighTempView.setContentDescription(mContext.getString(R.string.a11y_high_temp, highString));

        // Read low temperature from cursor
        double low = mCursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
        String lowString = WeatherUtils.formatTemperature(mContext, low);
        holder.mLowTempView.setText(lowString);
        holder.mLowTempView.setContentDescription(mContext.getString(R.string.a11y_low_temp, lowString));

        mICM.onBindViewHolder(holder, position);
    }

    void onRestoreInstanceState(Bundle savedInstanceState) {
        mICM.onRestoreInstanceState(savedInstanceState);
    }

    void onSaveInstanceState(Bundle outState) {
        mICM.onSaveInstanceState(outState);
    }

    void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
    }

    int getSelectedItemPosition() {
        return mICM.getSelectedItemPosition();
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }





    void selectView(RecyclerView.ViewHolder viewHolder) {
        if ( viewHolder instanceof ForecastViewHolder ) {
            ForecastViewHolder vfh = (ForecastViewHolder)viewHolder;
            vfh.onClick(vfh.itemView);
        }
    }

    public class ForecastViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final ImageView mIconView;
        final TextView mDateView;
        final TextView mDescriptionView;
        final TextView mHighTempView;
        final TextView mLowTempView;

        ForecastViewHolder(View view) {
            super(view);
            mIconView = view.findViewById(R.id.list_item_icon);
            mDateView = view.findViewById(R.id.list_item_date_textview);
            mDescriptionView = view.findViewById(R.id.list_item_forecast_textview);
            mHighTempView = view.findViewById(R.id.list_item_high_textview);
            mLowTempView = view.findViewById(R.id.list_item_low_textview);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            mCursor.moveToPosition(adapterPosition);
            int dateColumnIndex = mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE);
            mClickHandler.onClick(mCursor.getLong(dateColumnIndex), this);
            mICM.onClick(this);
        }
    }
    public interface ForecastAdapterOnClickHandler{
        void onClick(Long date, ForecastViewHolder vh);
    }


    ForecastAdapter(Context mContext, ForecastAdapterOnClickHandler mClickHandler,
                    View mEmptyView, int choiceMode) {
        this.mContext = mContext;
        this.mClickHandler = mClickHandler;
        this.mEmptyView = mEmptyView;
        mICM = new ItemChoiceManager(this);
        mICM.setChoiceMode(choiceMode);
    }

    @Override
    public int getItemCount() {
        if ( null == mCursor ) return 0;
        return mCursor.getCount();
    }

    void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
        mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    public Cursor getCursor() {
        return mCursor;
    }
}
