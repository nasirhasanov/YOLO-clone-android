package com.snikpik.android.adapter;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.snikpik.android.R;
import com.snikpik.android.ReplyInSnapActivity;
import com.snikpik.android.model.PollData;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ExpresserAdapter extends RecyclerView.Adapter<ExpresserAdapter.ViewHolder>{

private List<PollData> listData;
private FirebaseUser user;
private Context context;


public ExpresserAdapter(List<PollData> listData, Context context) {
        this.listData = listData;
        this.context = context;
}

@NonNull
@Override
public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.list_poll_result,parent,false);
        return new ViewHolder(view);
        }

@Override
public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        PollData ld=listData.get(position);
        holder.txtPollAnswer.setText(ld.getAnswer());
        holder.txtTimeStamp.setText(convertTimestamp(ld.getTimestamp()));

}

@Override
public int getItemCount() {
        return listData.size();
        }

class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private TextView txtPollAnswer;
    private ImageButton moreChoiceButton;
    private TextView replyInSnap, txtTimeStamp;

    ViewHolder(View itemView) {
        super(itemView);
        txtPollAnswer = itemView.findViewById(R.id.pollAnswer);
        txtTimeStamp = itemView.findViewById(R.id.replyTime);

        moreChoiceButton = itemView.findViewById(R.id.more_choices_result_list);
        moreChoiceButton.setOnClickListener(this);

        replyInSnap = itemView.findViewById(R.id.reply_in_snap);
        replyInSnap.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

        if (v.getId() == moreChoiceButton.getId()) {
            PollData ld = listData.get(getAdapterPosition());
            setMoreChoices(ld);
        }

        if (v.getId() == replyInSnap.getId()) {
            PollData ld = listData.get(getAdapterPosition());
            Intent intent = new Intent(context, ReplyInSnapActivity.class);
            intent.putExtra("anonymousText", ld.getAnswer());
            context.startActivity(intent);
        }
    }

    private void deleteAnswer(String answerId) {
        user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference answersDbRef = FirebaseDatabase.getInstance().getReference("results");
        answersDbRef.child(user.getUid())
                .child(answerId).setValue(null);
    }

    private void blockSender(String device_id, final String answer_id){
        user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference userDbRef = FirebaseDatabase.getInstance().getReference("users");
        final ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("blocking Sender ...");
        pd.show();
        userDbRef.child(user.getUid()).child("blocked").child(device_id)
                .setValue("true").addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                deleteAnswer(answer_id);
                pd.dismiss();
            }
        });
    }

    private void reportSender(final String device_id, final String answer_id, String answer){
        user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reportedDbRef = FirebaseDatabase.getInstance().getReference("reported");
        final ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("reporting ...");
        pd.show();

        reportedDbRef.child(device_id).child(user.getUid()).setValue(answer)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        pd.dismiss();
                        blockSender(device_id, answer_id);
                    }
                });
    }

    private void setMoreChoices(final PollData pollData){
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        final String[] choices = {
                context.getString(R.string.delete),
                context.getString(R.string.block_sender),
                context.getString(R.string.report)
        };
        builder.setItems(choices, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        deleteAnswer(pollData.getAnswerPath());
                        break;
                    case 1:
                        blockSender(pollData.getDevice_id(), pollData.getAnswerPath());
                        break;
                    case 2:
                        reportSender(pollData.getDevice_id(), pollData.getAnswerPath(),pollData.getAnswer());
                        break;
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
    private CharSequence convertTimestamp(String timestamp){
        return DateUtils.getRelativeTimeSpanString(Long.parseLong(timestamp),System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS);
    }




}