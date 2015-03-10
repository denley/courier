package me.denley.courier.sample;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.wearable.Node;

import java.util.List;

import me.denley.courier.Courier;
import me.denley.courier.ReceiveData;
import me.denley.courier.ReceiveMessages;
import me.denley.courier.RemoteNodes;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    TextView changeTextView, valueTextView, nodesTextView;
    View rootView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rootView = findViewById(R.id.root);
        changeTextView = (TextView) findViewById(android.R.id.text1);
        valueTextView = (TextView) findViewById(android.R.id.text2);
        nodesTextView = (TextView) findViewById(R.id.nodes);

        rootView.setOnClickListener(this);

        Courier.startReceiving(this);
    }

    @RemoteNodes
    void onConnectionChanged(List<Node> nodes) {
        nodesTextView.setText("Connected to "+nodes.size()+" devices");
    }

    @ReceiveMessages("/value_change")
    void onValueChange(final String changeDescription, final String nodeId) {
        changeTextView.setText(changeDescription);
    }

    @ReceiveData("/value")
    void onNewValue(final Integer value) {
        if(value==null) {
            valueTextView.setText(Integer.toString(0));
        } else {
            valueTextView.setText(Integer.toString(value));
        }
    }

    @Override public void onClick(View v) {
        for(Node node:remoteNodes) {
            Courier.deleteData(this, "/value", node.getId());
        }
    }

}
