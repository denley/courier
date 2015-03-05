package me.denley.courier.sample;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.wearable.Node;

import java.util.List;

import me.denley.courier.Courier;
import me.denley.courier.LocalNode;
import me.denley.courier.ReceiveData;
import me.denley.courier.ReceiveMessages;
import me.denley.courier.RemoteNodes;

public class MainActivity extends Activity  {

    @LocalNode
    Node localNode;

    TextView changeTextView, valueTextView, nodesTextView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        changeTextView = (TextView) findViewById(android.R.id.text1);
        valueTextView = (TextView) findViewById(android.R.id.text2);
        nodesTextView = (TextView) findViewById(R.id.nodes);

        Courier.startReceiving(this);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        Courier.stopReceiving(this);
    }

    @RemoteNodes
    void onConnectionChanged(List<Node> nodes) {
        nodesTextView.setText("Connected to "+nodes.size()+" devices");
    }

    @ReceiveMessages("/value_change")
    void onValueChange(final String changeDescription) {
        changeTextView.setText(changeDescription);
    }

    @ReceiveData("/value")
    void onNewValue(final int value) {
        valueTextView.setText(Integer.toString(value));
    }
}
