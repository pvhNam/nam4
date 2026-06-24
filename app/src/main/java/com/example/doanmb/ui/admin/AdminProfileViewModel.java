package com.example.doanmb.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.repository.UserRepository;
import com.example.doanmb.data.repository.WalletRepository;
import com.example.doanmb.ui.base.BaseViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** ViewModel cho trang cá nhân Admin: tên, email và số dư ví app. */
public class AdminProfileViewModel extends BaseViewModel {

    private final UserRepository userRepo = UserRepository.getInstance();
    private final WalletRepository walletRepo = WalletRepository.getInstance();

    private final MutableLiveData<String> name = new MutableLiveData<>();
    private final MutableLiveData<String> email = new MutableLiveData<>();
    private final MutableLiveData<Long> appWallet = new MutableLiveData<>();

    public LiveData<String> getName()     { return name; }
    public LiveData<String> getEmail()    { return email; }
    public LiveData<Long> getAppWallet()  { return appWallet; }

    public void load() {
        walletRepo.getAppWalletBalance(r -> { if (r.isSuccess()) appWallet.setValue(r.getData()); });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        if (user.getEmail() != null) email.setValue(user.getEmail());
        userRepo.getUserDoc(user.getUid(), r -> {
            if (r.isSuccess() && r.getData() != null) {
                String n = r.getData().getString(F.NAME);
                if (n != null && !n.isEmpty()) name.setValue(n);
            }
        });
    }
}
