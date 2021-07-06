package com.example.cart.listener;

import com.example.cart.model.ItemModel;

import java.util.List;

public interface IItemLoadListener {
    void onItemLoadSuccess(List<ItemModel> itemModelList);
    void onItemLoadFailed(String message);
}
