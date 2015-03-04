package me.denley.courier.sample;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import me.denley.courier.Courier;
import me.denley.courier.ReceiveData;
import me.denley.courier.ReceiveMessages;

public class MainActivity extends Activity  {

    TextView changeTextView, valueTextView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        changeTextView = (TextView) findViewById(android.R.id.text1);
        valueTextView = (TextView) findViewById(android.R.id.text2);

        Courier.startReceiving(this);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        Courier.stopReceiving(this);
    }

    @ReceiveMessages("/value_change")
    void onValueChange(String changeDescription) {
        changeTextView.setText(changeDescription);
    }

    @ReceiveData("/value")
    void onNewValue(int value) {
        valueTextView.setText(Integer.toString(value));
    }
}
