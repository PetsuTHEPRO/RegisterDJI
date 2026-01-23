package edu.ifma.ifma_sdia.views;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import edu.ifma.ifma_sdia.R;
import edu.ifma.ifma_sdia.controllers.MainClientController;
import edu.ifma.ifma_sdia.controllers.VideoFeedController;

public class MainPageActivity extends AppCompatActivity {

    private VideoFeedController videoFeedController;
    private MainClientController mainClientController;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        String host = "127.0.0.1";
        int mainPort = 6564;
        int videoPort = 6565;
        videoFeedController = new VideoFeedController(host, videoPort);
        mainClientController = new MainClientController(host, mainPort);

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(videoFeedController!=null)
            videoFeedController.disconnect();
    }
}