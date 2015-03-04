package me.denley.courier.sample;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;

import me.denley.courier.Courier;
import me.denley.courier.ReceiveData;

public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    TextView valueText;

    int value = 0;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        valueText = (TextView) findViewById(android.R.id.text1);
        findViewById(R.id.up).setOnClickListener(this);
        findViewById(R.id.down).setOnClickListener(this);

        Courier.startReceiving(this);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        Courier.stopReceiving(this);
    }

    @ReceiveData("/value")
    void onNewValue(final int value) {
        this.value = value;
        valueText.setText(Integer.toString(value));
    }

    @Override public void onClick(View v) {
        switch(v.getId()) {
            case R.id.up:
                Courier.deliverMessage(this, "/value_change", getString(R.string.action_up));
                value++;
                break;
            case R.id.down:
                Courier.deliverMessage(this, "/value_change", getString(R.string.action_down));
                value--;
                break;
        }

        Courier.deliverData(this, "/value", value);
    }
}
