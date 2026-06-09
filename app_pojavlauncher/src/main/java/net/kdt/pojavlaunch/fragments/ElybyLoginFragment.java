package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class ElybyLoginFragment extends Fragment {
    public static final String TAG = "ELYBY_LOGIN_FRAGMENT";

    private EditText mUsernameEditText;
    private EditText mPasswordEditText;
    private View mLoginButton;

    public ElybyLoginFragment() {
        super(R.layout.fragment_elyby_login);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mUsernameEditText = view.findViewById(R.id.login_edit_email);
        mPasswordEditText = view.findViewById(R.id.login_edit_password);
        mLoginButton = view.findViewById(R.id.login_button);

        mLoginButton.setOnClickListener(v -> performLogin());
    }

    private void performLogin() {
        String username = mUsernameEditText.getText().toString().trim();
        String password = mPasswordEditText.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        mLoginButton.setEnabled(false);
        Toast.makeText(requireContext(), "Authenticating with Ely.by...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                URL url = new URL("https://authserver.ely.by/auth/authenticate");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                JSONObject agent = new JSONObject();
                agent.put("name", "Minecraft");
                agent.put("version", 1);
                payload.put("agent", agent);
                payload.put("username", username);
                payload.put("password", password);
                payload.put("requestUser", true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                Scanner s = new Scanner(is).useDelimiter("\\A");
                String responseStr = s.hasNext() ? s.next() : "";

                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject responseJson = new JSONObject(responseStr);
                    String accessToken = responseJson.getString("accessToken");
                    String clientToken = responseJson.getString("clientToken");
                    JSONObject selectedProfile = responseJson.getJSONObject("selectedProfile");
                    String profileId = selectedProfile.getString("id");
                    String profileName = selectedProfile.getString("name");

                    MinecraftAccount account = new MinecraftAccount();
                    account.username = profileName;
                    account.accessToken = accessToken;
                    account.clientToken = clientToken;
                    account.profileId = profileId;
                    account.isMicrosoft = false;

                    // Ely.by skins: we trigger a skin update immediately
                    account.updateSkinFace();

                    account.save();

                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(requireContext(), "Ely.by Login Successful!", Toast.LENGTH_SHORT).show();
                        Tools.swapFragment(requireActivity(), MainMenuFragment.class, MainMenuFragment.TAG, null);
                    });

                } else {
                    String errorMessage = "Login failed";
                    try {
                        JSONObject errorJson = new JSONObject(responseStr);
                        if (errorJson.has("errorMessage")) {
                            errorMessage = errorJson.getString("errorMessage");
                        }
                    } catch (JSONException e) {
                        // ignore
                    }
                    final String finalError = errorMessage;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        mLoginButton.setEnabled(true);
                        Tools.dialog(requireContext(), "Error", finalError);
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Ely.by login exception", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    mLoginButton.setEnabled(true);
                    Tools.showError(requireContext(), e);
                });
            }
        }).start();
    }
}
