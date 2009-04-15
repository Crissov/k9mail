
package com.android.email.activity.setup;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.email.Account;
import com.android.email.Email;
import com.android.email.R;
import com.android.email.mail.AuthenticationFailedException;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Store;
import com.android.email.mail.Transport;
import com.android.email.mail.CertificateValidationException;
import com.android.email.mail.store.TrustManagerFactory;

/**
 * Checks the given settings to make sure that they can be used to send and
 * receive mail.
 *
 * XXX NOTE: The manifest for this app has it ignore config changes, because
 * it doesn't correctly deal with restarting while its thread is running.
 */
public class AccountSetupCheckSettings extends Activity implements OnClickListener {
	
	public static final int ACTIVITY_REQUEST_CODE = 1;
	
    private static final String EXTRA_ACCOUNT = "account";

    private static final String EXTRA_CHECK_INCOMING = "checkIncoming";

    private static final String EXTRA_CHECK_OUTGOING = "checkOutgoing";

    private Handler mHandler = new Handler();

    private ProgressBar mProgressBar;

    private TextView mMessageView;

    private Account mAccount;

    private boolean mCheckIncoming;

    private boolean mCheckOutgoing;

    private boolean mCanceled;

    private boolean mDestroyed;

    public static void actionCheckSettings(Activity context, Account account,
            boolean checkIncoming, boolean checkOutgoing) {
        Intent i = new Intent(context, AccountSetupCheckSettings.class);
        i.putExtra(EXTRA_ACCOUNT, account);
        i.putExtra(EXTRA_CHECK_INCOMING, checkIncoming);
        i.putExtra(EXTRA_CHECK_OUTGOING, checkOutgoing);
        context.startActivityForResult(i, ACTIVITY_REQUEST_CODE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_check_settings);
        mMessageView = (TextView)findViewById(R.id.message);
        mProgressBar = (ProgressBar)findViewById(R.id.progress);
        ((Button)findViewById(R.id.cancel)).setOnClickListener(this);

        setMessage(R.string.account_setup_check_settings_retr_info_msg);
        mProgressBar.setIndeterminate(true);

        mAccount = (Account)getIntent().getSerializableExtra(EXTRA_ACCOUNT);
        mCheckIncoming = (boolean)getIntent().getBooleanExtra(EXTRA_CHECK_INCOMING, false);
        mCheckOutgoing = (boolean)getIntent().getBooleanExtra(EXTRA_CHECK_OUTGOING, false);

        new Thread() {
            public void run() {
            	Store store = null;
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                try {
                    if (mDestroyed) {
                        return;
                    }
                    if (mCanceled) {
                        finish();
                        return;
                    }
                    if (mCheckIncoming) {
                    	setMessage(R.string.account_setup_check_settings_check_incoming_msg);
                    	store = Store.getInstance(mAccount.getStoreUri(), getApplication());
                    	store.checkSettings();
                    }
                    if (mDestroyed) {
                        return;
                    }
                    if (mCanceled) {
                        finish();
                        return;
                    }
                    if (mCheckOutgoing) {
                        setMessage(R.string.account_setup_check_settings_check_outgoing_msg);
                        Transport transport = Transport.getInstance(mAccount.getTransportUri());
                        transport.close();
                        transport.open();
                        transport.close();
                    }
                    if (mDestroyed) {
                        return;
                    }
                    if (mCanceled) {
                        finish();
                        return;
                    }
                    setResult(RESULT_OK);
                    finish();
                } catch (final AuthenticationFailedException afe) {
                  Log.e(Email.LOG_TAG, "Error while testing settings", afe);
                    showErrorDialog(
                            R.string.account_setup_failed_dlg_auth_message_fmt,
                            afe.getMessage() == null ? "" : afe.getMessage());
                } catch (final CertificateValidationException cve) {
                  Log.e(Email.LOG_TAG, "Error while testing settings", cve);
                	acceptKeyDialog(
                            R.string.account_setup_failed_dlg_certificate_message_fmt,
                            cve);
                } catch (final Throwable t) {
                  Log.e(Email.LOG_TAG, "Error while testing settings", t);
                    showErrorDialog(
                            R.string.account_setup_failed_dlg_server_message_fmt,
                            (t.getMessage() == null ? "" : t.getMessage()));
                	
                }
            }

        }.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDestroyed = true;
        mCanceled = true;
    }

    private void setMessage(final int resId) {
        mHandler.post(new Runnable() {
            public void run() {
                if (mDestroyed) {
                    return;
                }
                mMessageView.setText(getString(resId));
            }
        });
    }

    private void showErrorDialog(final int msgResId, final Object... args) {
        mHandler.post(new Runnable() {
            public void run() {
                if (mDestroyed) {
                    return;
                }
                mProgressBar.setIndeterminate(false);
                new AlertDialog.Builder(AccountSetupCheckSettings.this)
                        .setTitle(getString(R.string.account_setup_failed_dlg_title))
                        .setMessage(getString(msgResId, args))
                        .setCancelable(true)
                        .setPositiveButton(
                                getString(R.string.account_setup_failed_dlg_edit_details_action),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                        .show();
            }
        });
    }
    private void acceptKeyDialog(final int msgResId, final Object... args) {
        mHandler.post(new Runnable() {
            public void run() {
                if (mDestroyed) {
                    return;
                }
                final X509Certificate[] chain = TrustManagerFactory.getLastCertChain();
                String exMessage = "Unknown Error";
                
                Exception ex = ((Exception)args[0]);
                if (ex != null) {
                	if (ex.getCause() != null) {
                    	if (ex.getCause().getCause() != null) {
                    		exMessage = ex.getCause().getCause().getMessage();
                    		
                    	} else {
                    		exMessage = ex.getCause().getMessage();
                    	}
                	} else {
                		exMessage = ex.getMessage();
                	}
                }
                
                mProgressBar.setIndeterminate(false);
                StringBuffer chainInfo = new StringBuffer(100);
                for (int i = 0; i < chain.length; i++)
                {
                   // display certificate chain information
                    chainInfo.append("Certificate chain[" + i + "]:\n");
                    chainInfo.append("Subject: " + chain[i].getSubjectDN().toString() + "\n");
                    chainInfo.append("Issuer: " + chain[i].getIssuerDN().toString() + "\n");
                }

                new AlertDialog.Builder(AccountSetupCheckSettings.this)
                        .setTitle(getString(R.string.account_setup_failed_dlg_invalid_certificate_title))
                        //.setMessage(getString(R.string.account_setup_failed_dlg_invalid_certificate)
                        .setMessage(getString(msgResId,exMessage)
                        		+ " " + chainInfo.toString()
                        		)
                        .setCancelable(true)
                        .setPositiveButton(
                        		getString(R.string.account_setup_failed_dlg_invalid_certificate_accept),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    	try {
                                    		String alias = mAccount.getUuid();
                                    		if (mCheckIncoming) {
                                    			alias = alias + ".incoming";
                                    		}
                                    		if (mCheckOutgoing) {
                                    			alias = alias + ".outgoing";
                                    		}
											TrustManagerFactory.addCertificateChain(alias, chain);
										} catch (CertificateException e) {
						                	showErrorDialog(
						                            R.string.account_setup_failed_dlg_certificate_message_fmt,
						                            e.getMessage() == null ? "" : e.getMessage());											
										}
                                    	AccountSetupCheckSettings.actionCheckSettings(AccountSetupCheckSettings.this, mAccount,
                                                mCheckIncoming, mCheckOutgoing);
                                    }
                                })
                        .setNegativeButton(
                        		getString(R.string.account_setup_failed_dlg_invalid_certificate_reject),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                        .show();
            }
        });
    }

    public void onActivityResult(int reqCode, int resCode, Intent data) {
    	setResult(resCode);
    	finish();
    }

    
    private void onCancel() {
        mCanceled = true;
        setMessage(R.string.account_setup_check_settings_canceling_msg);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
                onCancel();
                break;
        }
    }
}
