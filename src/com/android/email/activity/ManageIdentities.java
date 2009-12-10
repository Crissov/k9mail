package com.android.email.activity;

import android.content.Intent;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;
import com.android.email.Account;
import com.android.email.Preferences;
import com.android.email.R;

public class ManageIdentities extends ChooseIdentity
{
    private boolean mIdentitiesChanged = false;
    public static final String EXTRA_IDENTITIES = "com.android.email.EditIdentity_identities";

    private static final int ACTIVITY_EDIT_IDENTITY = 1;
    protected void setupClickListeners()
    {
        this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView adapterview, View view, int i, long l)
            {
                editItem(i);
            }
        });

        ListView listView = getListView();
        registerForContextMenu(listView);
    }

    private void editItem(int i)
    {
        Intent intent = new Intent(ManageIdentities.this, EditIdentity.class);

        intent.putExtra(EditIdentity.EXTRA_ACCOUNT, mAccount);
        intent.putExtra(EditIdentity.EXTRA_IDENTITY, mAccount.getIdentity(i));
        intent.putExtra(EditIdentity.EXTRA_IDENTITY_INDEX, i);
        startActivityForResult(intent, ACTIVITY_EDIT_IDENTITY);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.manage_identities_option, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.new_identity:
                Intent intent = new Intent(ManageIdentities.this, EditIdentity.class);

                intent.putExtra(EditIdentity.EXTRA_ACCOUNT, mAccount);
                startActivityForResult(intent, ACTIVITY_EDIT_IDENTITY);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.manage_identities_context_menu_title);
        getMenuInflater().inflate(R.menu.manage_identities_context, menu);
    }

    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
        switch (item.getItemId())
        {
            case R.id.edit:
                editItem(menuInfo.position);
                break;
            case R.id.up:
                if (menuInfo.position > 0)
                {
                    Account.Identity identity = identities.remove(menuInfo.position);
                    identities.add(menuInfo.position - 1, identity);
                    mIdentitiesChanged = true;
                    refreshView();
                }

                break;
            case R.id.down:
                if (menuInfo.position < identities.size() - 1)
                {
                    Account.Identity identity = identities.remove(menuInfo.position);
                    identities.add(menuInfo.position + 1, identity);
                    mIdentitiesChanged = true;
                    refreshView();
                }
                break;
            case R.id.top:
                Account.Identity identity = identities.remove(menuInfo.position);
                identities.add(0, identity);
                mIdentitiesChanged = true;
                refreshView();
                break;
            case R.id.remove:
                if (identities.size() > 1)
                {
                    identities.remove(menuInfo.position);
                    mIdentitiesChanged = true;
                    refreshView();
                }
                else
                {
                    Toast.makeText(this, getString(R.string.no_removable_identity),
                                   Toast.LENGTH_LONG).show();
                }
                break;
        }
        return true;
    }


    @Override
    public void onResume()
    {
        super.onResume();
        mAccount.refresh(Preferences.getPreferences(getApplication().getApplicationContext()));
        refreshView();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            saveIdentities();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void saveIdentities()
    {
        if (mIdentitiesChanged)
        {
            mAccount.setIdentities(identities);
            mAccount.save(Preferences.getPreferences(getApplication().getApplicationContext()));
        }
        finish();
    }
}
