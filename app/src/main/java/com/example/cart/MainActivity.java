package com.example.cart;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.example.cart.adapter.MyItemAdapter;
import com.example.cart.eventbus.MyUpdateCartEvent;
import com.example.cart.listener.ICartLoadListener;
import com.example.cart.listener.IItemLoadListener;
import com.example.cart.model.CartModel;
import com.example.cart.model.ItemModel;
import com.example.cart.utils.SpaceItemDecoration;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nex3z.notificationbadge.NotificationBadge;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.cart.R.*;

public class MainActivity extends AppCompatActivity implements IItemLoadListener, ICartLoadListener {

    @BindView(R.id.recycler_item)
    RecyclerView recyclerItem;

    @BindView(R.id.mainLayout)
    RelativeLayout mainLayout;

    @BindView(R.id.badge)
    NotificationBadge badge;

    @BindView(R.id.btnCart)
    FrameLayout btnCart;

    IItemLoadListener itemLoadListener;
    ICartLoadListener cartLoadListener;

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        if (EventBus.getDefault().hasSubscriberForEvent(MyUpdateCartEvent.class))
            EventBus.getDefault().removeStickyEvent(MyUpdateCartEvent.class);
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
    public void onUpdateCart(MyUpdateCartEvent event)
    {
      countCartItem();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);


        init();
        loadItemFromFirebase();
        countCartItem();
    }


    //Load Devices from Firebase Databse
    private void loadItemFromFirebase() {
        List<ItemModel> itemModels = new ArrayList<>();
        FirebaseDatabase.getInstance()
                .getReference("Item")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists())
                        {
                            for (DataSnapshot itemSnapshot:snapshot.getChildren())
                            {
                                ItemModel itemModel = itemSnapshot.getValue(ItemModel.class);
                                itemModel.setKey(itemSnapshot.getKey());
                                itemModels.add(itemModel);
                            }
                            itemLoadListener.onItemLoadSuccess(itemModels);
                        }
                        else
                            itemLoadListener.onItemLoadFailed("Can't Find Device");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        itemLoadListener.onItemLoadFailed(error.getMessage());
                    }
                });
    }

    private void init() {
        ButterKnife.bind(this);

        itemLoadListener = this;
        cartLoadListener = this;

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this,2);
        recyclerItem.setLayoutManager(gridLayoutManager);
        recyclerItem.addItemDecoration(new SpaceItemDecoration());

        btnCart.setOnClickListener(v -> startActivity(new Intent(this,CartActivity.class)));
    }

    @Override
    public void onItemLoadSuccess(List<ItemModel> itemModelList) {
        MyItemAdapter adapter = new MyItemAdapter(this,itemModelList,cartLoadListener);
        recyclerItem.setAdapter(adapter);
    }

    @Override
    public void onItemLoadFailed(String message) {
        Snackbar.make(mainLayout,message, Snackbar.LENGTH_LONG).show();

    }

    //calculations

    @Override
    public void onCartLoadSuccess(List<CartModel> cartModelList) {
        int cartSum = 0;
        for(CartModel cartModel: cartModelList)
            cartSum += cartModel.getQuantity();
        badge.setNumber(cartSum);

    }

    @Override
    public void onCartLoadFailed(String message) {
        Snackbar.make(mainLayout,message,Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        countCartItem();
    }


    //count Cart Items
    private void countCartItem() {
        List<CartModel> cartModels = new ArrayList<>();
                FirebaseDatabase
                        .getInstance().getReference("Cart")
                        .child("UNIQUE_USER_ID")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                for (DataSnapshot cartSnapshot:snapshot.getChildren())
                                {
                                    CartModel cartModel = cartSnapshot.getValue(CartModel.class);
                                    cartModel.setKey(cartSnapshot.getKey());
                                    cartModels.add(cartModel);
                                }
                                cartLoadListener.onCartLoadSuccess(cartModels);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                cartLoadListener.onCartLoadFailed(error.getMessage());
                            }
                        });
    }
}