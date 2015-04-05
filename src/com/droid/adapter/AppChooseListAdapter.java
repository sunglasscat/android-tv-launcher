package com.droid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;

import com.droid.R;
import com.droid.bean.AppBean;


public class AppChooseListAdapter extends BaseAdapter
{
  private ArrayList<AppBean> beans;
  private Context context;

  public AppChooseListAdapter(Context paramContext, ArrayList<AppBean> paramArrayList)
  {
    this.context = paramContext;
    this.beans = paramArrayList;
  }

  public int getCount()
  {
    return beans.size();
  }

  public Object getItem(int paramInt)
  {
    return beans.get(paramInt);
  }

  public long getItemId(int paramInt)
  {
    return paramInt;
  }

  public View getView(int paramInt, View paramView, ViewGroup paramViewGroup)
  {
    if (paramView == null)
    {
      paramView = LayoutInflater.from(context).inflate(R.layout.item_news, null);
    }
    ImageView appIcon = ((ImageView)paramView.findViewById(R.id.news_img));
    TextView appName = ((TextView)paramView.findViewById(R.id.news_text));
    while (true)
    {
      AppBean localAppBean = (AppBean)beans.get(paramInt);
      appName.setText(localAppBean.getName());
      appIcon.setImageDrawable(localAppBean.getIcon());
      return paramView;
    }
  }
}