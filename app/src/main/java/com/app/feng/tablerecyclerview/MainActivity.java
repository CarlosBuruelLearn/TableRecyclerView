package com.app.feng.tablerecyclerview;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.app.feng.fixtablelayout.FixTableLayout;
import com.app.feng.fixtablelayout.inter.ILoadMoreListener;
import com.app.feng.tablerecyclerview.bean.DataBean;

import java.util.ArrayList;
import java.util.List;

public class MainActivity
  extends AppCompatActivity
{
  //Lista de titulos
	public String[] title = {"title1","title2","title3","title4","title5","title6","title7", "title8","title9"};

  public List<DataBean> data = new ArrayList<>();

  int currentPage = 1;
  int totalPage = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      for (int i = 0; i < 15;)
      {
        data.add(new DataBean("id__" + i++,"data1","data2","data3","data4","data5","data6","data7", "data8"));
      }
	    //https://stackoverflow.com/questions/16645164/how-to-implement-the-android-zoomview
	    View view = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
	    .inflate(R.layout.tablelayout, null, false);
	    view.setLayoutParams(new RelativeLayout.LayoutParams(
		    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

	    RelativeLayout linearLayout = (RelativeLayout) findViewById(R.id.relativeLayout);
	    linearLayout.addView(view);
      //Add zoom control
      final ZoomControls zoomControls = new ZoomControls(this);
	    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
		    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
	    layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
      zoomControls.setLayoutParams(layoutParams);

      linearLayout.addView(zoomControls);

      final FixTableLayout fixTableLayout = (FixTableLayout) view.findViewById(R.id.fixTableLayout);

	    zoomControls.setOnZoomInClickListener(new View.OnClickListener()
	    {
		    @Override
		    public void onClick(View view)
		    {
			    float x = fixTableLayout.getScaleX();
			    float y = fixTableLayout.getScaleY();
			    fixTableLayout.setScaleX(x + 1);
			    fixTableLayout.setScaleY(y + 1);
			    fixTableLayout.setPivotX(0);
			    fixTableLayout.setPivotY(0);

		    }
	    });
	    zoomControls.setOnZoomOutClickListener(new View.OnClickListener()
	    {
		    @Override
		    public void onClick(View view)
		    {
			    float x = fixTableLayout.getScaleX();
			    float y = fixTableLayout.getScaleY();

			    if( x > 1 )
			    {
				    fixTableLayout.setScaleX(x - 1);
				    fixTableLayout.setScaleY(y - 1);
				    fixTableLayout.setPivotX(0);
				    fixTableLayout.setPivotY(0);
			    }
		    }
	    });

      // 一定要设置Adapter 否则看不到TableLayout
      final FixTableAdapter fixTableAdapter = new FixTableAdapter(title,data);
      fixTableLayout.setAdapter(fixTableAdapter);

	    if(true)return;
      fixTableLayout.enableLoadMoreData();

      fixTableLayout.setLoadMoreListener(new ILoadMoreListener()
      {
        @Override
        public void loadMoreData(final Handler handler)
        {
          Log.i("feng"," 更新了Data --- ");
          final Message message = handler.obtainMessage(FixTableLayout.MESSAGE_FIX_TABLE_LOAD_COMPLETE);

          new Thread(new Runnable()
          {
            @Override
            public void run()
            {
              if (currentPage <= totalPage)
              {
                for (int i = 0; i < 2; i++)
                {
	                data.add(new DataBean("update_id","update_data","data2","data3","data4","data5", "data6","data7","data8"));
                }
                currentPage++;
                message.arg1 = 2;
              }
              else
              {
	              message.arg1 = 0;
              }
              handler.sendMessage(message);
            }
          }).start();
        }
      });
  }
}
