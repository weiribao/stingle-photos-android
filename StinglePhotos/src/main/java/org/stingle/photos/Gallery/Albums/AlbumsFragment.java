package org.stingle.photos.Gallery.Albums;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import org.stingle.photos.AsyncTasks.OnAsyncTaskFinish;
import org.stingle.photos.Crypto.Crypto;
import org.stingle.photos.Crypto.CryptoException;
import org.stingle.photos.Db.Objects.StingleDbAlbum;
import org.stingle.photos.Gallery.Helpers.AutoFitGridLayoutManager;
import org.stingle.photos.Gallery.Helpers.GalleryHelpers;
import org.stingle.photos.GalleryActivity;
import org.stingle.photos.R;
import org.stingle.photos.StinglePhotosApplication;
import org.stingle.photos.Util.Helpers;

import java.io.IOException;
import java.util.Objects;

public class AlbumsFragment extends Fragment{

	public static int VIEW_ALL = 0;
	public static int VIEW_ALBUMS = 1;
	public static int VIEW_SHARES = 2;

	private RecyclerView recyclerView;
	private AlbumsAdapterPisasso adapter;
	private LinearLayoutManager layoutManager;
	private Integer view = VIEW_ALBUMS;;

	private int lastScrollPosition = 0;

	private GalleryActivity parentActivity;

	private AlbumsAdapterPisasso.Listener adapterListener = new AlbumsAdapterPisasso.Listener() {
		@Override
		public void onClick(int index, int type) {
			if(type == AlbumsAdapterPisasso.TYPE_ADD){
				GalleryHelpers.addAlbum(getContext(), new OnAsyncTaskFinish() {
					@Override
					public void onFinish(Object set) {
						super.onFinish();
						updateDataSet();
					}

					@Override
					public void onFail() {
						super.onFail();
						Helpers.showAlertDialog(getContext(), getString(R.string.add_album_failed));
					}
				});
			}
			else{
				StingleDbAlbum album = adapter.getAlbumAtPosition(index);
				String albumName = "";
				try {
					Crypto.AlbumData albumData = StinglePhotosApplication.getCrypto().parseAlbumData(album.publicKey, album.encPrivateKey, album.metadata);
					albumName = albumData.name;
					albumData = null;
				} catch (IOException | CryptoException e) {
					e.printStackTrace();
				}
				parentActivity.showAlbum(album.albumId, albumName);
			}
		}

		@Override
		public void onLongClick(int index, int type) {

		}

		@Override
		public void onSelectionChanged(int count) {

		}
	};


	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.albums_fragment,	container, false);

		recyclerView = view.findViewById(R.id.recycler_view);
		parentActivity = (GalleryActivity)getActivity();

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Bundle bundle = getArguments();
		if (bundle != null) {
			view = bundle.getInt("view", VIEW_ALBUMS);
		}

		((SimpleItemAnimator) Objects.requireNonNull(recyclerView.getItemAnimator())).setSupportsChangeAnimations(false);
		recyclerView.setHasFixedSize(true);
		if(view == VIEW_ALBUMS) {
			layoutManager = new AutoFitGridLayoutManager(getContext(), Helpers.getThumbSize(getContext(), 2));
		}
		else if(view == VIEW_SHARES){
			layoutManager = new LinearLayoutManager(getContext());
		}
		recyclerView.setLayoutManager(layoutManager);
		if(view == VIEW_ALBUMS) {
			adapter = new AlbumsAdapterPisasso(getContext(), layoutManager, view, true, false);
			adapter.setLayoutStyle(AlbumsAdapterPisasso.LAYOUT_GRID);
		}
		else if(view == VIEW_SHARES){
			adapter = new AlbumsAdapterPisasso(getContext(), layoutManager, view, false, false);
			adapter.setLayoutStyle(AlbumsAdapterPisasso.LAYOUT_LIST);
		}
		adapter.setListener(adapterListener);

		if(savedInstanceState != null && savedInstanceState.containsKey("scroll")){
			lastScrollPosition = savedInstanceState.getInt("scroll");
		}

		recyclerView.setAdapter(adapter);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt("scroll", layoutManager.findFirstVisibleItemPosition());
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.e("function", "onResume");
		if (adapter != null) {
			adapter.updateDataSet();
		}

		layoutManager.scrollToPosition(lastScrollPosition);
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.e("function", "onPause");
		lastScrollPosition = layoutManager.findFirstVisibleItemPosition();
		recyclerView.setAdapter(null);
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		Log.e("function", "onAttach");
	}

	@Override
	public void onDetach() {
		super.onDetach();
		Log.e("function", "onDetach");
	}

	public void updateDataSet(){
		int lastScrollPos = recyclerView.getScrollY();
		adapter.updateDataSet();
		recyclerView.setScrollY(lastScrollPos);
	}

	public void updateItem(int position){
		adapter.updateItem(position);
	}

	public void scrollToTop(){
		lastScrollPosition = 0;
		recyclerView.scrollToPosition(0);
	}

	public void updateAutoFit(){
		if(view == VIEW_ALBUMS) {
			((AutoFitGridLayoutManager)layoutManager).updateAutoFit();
		}
	}

	public static Boolean getAlbumIsHiddenByView(Integer view){
		Boolean isHidden = null;
		if(view == AlbumsFragment.VIEW_ALBUMS){
			isHidden = false;
		}
		else if(view == AlbumsFragment.VIEW_SHARES){
			isHidden = true;
		}
		return isHidden;
	}

	public static Boolean getAlbumIsSharedByView(Integer view){
		Boolean isShared = null;
		if(view == AlbumsFragment.VIEW_SHARES){
			isShared = true;
		}
		return isShared;
	}

}
