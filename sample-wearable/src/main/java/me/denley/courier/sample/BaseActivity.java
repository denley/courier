package me.denley.courier.sample;

import android.app.Activity;

import com.google.android.gms.wearable.Node;

import java.util.ArrayList;
import java.util.List;

import me.denley.courier.Courier;
import me.denley.courier.LocalNode;
import me.denley.courier.RemoteNodes;

public class BaseActivity extends Activity {

    @LocalNode
    Node localNode;

    @RemoteNodes
    List<Node> remoteNodes = new ArrayList<Node>();

    @Override protected void onDestroy() {
        super.onDestroy();
        Courier.stopReceiving(this);
    }

}
