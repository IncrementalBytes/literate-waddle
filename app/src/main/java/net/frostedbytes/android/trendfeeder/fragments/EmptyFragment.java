package net.frostedbytes.android.trendfeeder.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import net.frostedbytes.android.trendfeeder.BaseActivity;
import net.frostedbytes.android.trendfeeder.R;
import net.frostedbytes.android.trendfeeder.utils.LogUtils;

public class EmptyFragment extends Fragment {

  private static final String TAG = EmptyFragment.class.getSimpleName();

  private String mMessage;

  public static EmptyFragment newInstance(@Nullable String message) {

    LogUtils.debug(TAG, "++newInstance()");
    EmptyFragment fragment = new EmptyFragment();
    Bundle args = new Bundle();
    args.putString(BaseActivity.ARG_EMPTY_MESSAGE, message);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    Bundle arguments = getArguments();
    if (arguments != null) {
      mMessage = arguments.getString(BaseActivity.ARG_EMPTY_MESSAGE);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    View view = inflater.inflate(R.layout.fragment_empty, container, false);
    TextView emptyTextView = view.findViewById(R.id.empty_text_default);
    emptyTextView.setText(mMessage);
    return view;
  }
}
