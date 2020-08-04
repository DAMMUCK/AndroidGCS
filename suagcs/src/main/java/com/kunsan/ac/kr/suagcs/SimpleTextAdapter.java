package com.kunsan.ac.kr.suagcs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.ViewHolder> {
private ArrayList<String> mData = null;

public class ViewHolder extends RecyclerView.ViewHolder{
    TextView textView1;

    //목록의 뷰는 ViewHolder 객체로 표현된다.
    //객체 : RecyclerView.ViewHolder를 확장해서 정의한 클래스의 인스턴스
    public ViewHolder(@NonNull View itemView){
        super(itemView);
        textView1 = itemView.findViewById(R.id.text1);
    }
}

    //생성자에서 데이터 리스트 객체 전달받음.
    SimpleTextAdapter(ArrayList<String> list){
        mData = list;
    }

    //onCreateViewHolder() - 아이템 뷰를위한 뷰홀더 객체 생성하여 리턴.
    //create new views(invoked by the layout manager)
    @NonNull
    @Override
    public SimpleTextAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //서브화면을 보여줄 inflater 객체 생성
        //inflater.inflate(sub 페이지, sub화면이 들어갈 fraim id, true); , true = 바로적용한다는 뜻
        View view = inflater.inflate(R.layout.recyclerview_item,parent, false);
        //뷰홀더 객체
        // -> 이러한 객체는 RecyclerView.Adapter를 확장해 만든 adapter에서 관리한다.
        SimpleTextAdapter.ViewHolder vh = new SimpleTextAdapter.ViewHolder(view);
        return vh;
    }

    //onBindViewHolder() - position에 해당하는 데이터를 뷰홀더의 아이템뷰에 표시
    @Override
    public void onBindViewHolder(@NonNull SimpleTextAdapter.ViewHolder holder, int position){
        String text = mData.get(position);
        holder.textView1.setText(text);
    }

    //getItemCount() - 전체 데이터 개수 리턴
    @Override
    public int getItemCount(){
        return mData.size();
    }

}
