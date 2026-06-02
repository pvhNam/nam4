package com.example.doanmb.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmb.R;
import com.example.doanmb.adapter.NotificationAdapter;
import com.example.doanmb.model.Notification;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView rvNotifications;

    private NotificationAdapter adapter;

    private List<Notification> list =
            new ArrayList<>();

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_notifications,
                container,
                false);

        rvNotifications =
                view.findViewById(
                        R.id.rvNotifications);

        rvNotifications.setLayoutManager(
                new LinearLayoutManager(getContext()));

        adapter =
                new NotificationAdapter(list);

        rvNotifications.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        loadNotifications();

        return view;
    }

    private void loadNotifications() {

        db.collection("notifications")
                .get()
                .addOnSuccessListener(snapshots -> {

                    list.clear();

                    for (QueryDocumentSnapshot doc : snapshots) {

                        Notification notification =
                                doc.toObject(
                                        Notification.class);

                        list.add(notification);
                    }

                    adapter.notifyDataSetChanged();
                });
    }
}