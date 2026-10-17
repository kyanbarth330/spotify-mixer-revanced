package org.example.spotifymixer.runtime;
import android.app.*;import android.text.InputType;import android.view.*;import android.widget.*;import org.example.spotifymixer.ui.EnhancerPanel;
final class AccountPanel {
 static void render(Activity activity,LinearLayout body){
  EnhancerPanel.label(body,"Your SpotEnhancer account",26,EnhancerPanel.WHITE,true);EnhancerPanel.label(body,Auth.status(),14,EnhancerPanel.MUTED,false);
  EnhancerPanel.label(body,"SpotEnhancer Premium unlocks our tools. It does not grant Spotify Premium, downloads or other Spotify account rights.",12,EnhancerPanel.MUTED,false);
  if(Auth.signedIn()){
   if(Auth.premiumExpiry()>0)EnhancerPanel.label(body,"Membership expiry: "+java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(Auth.premiumExpiry()*1000)),12,EnhancerPanel.MUTED,false);
   EnhancerPanel.button(body,"Refresh membership",true,Auth::refresh).setEnabled(!Auth.busy());
   EnhancerPanel.button(body,"Sign out",false,()->Auth.logout(false)).setEnabled(!Auth.busy());
   EnhancerPanel.button(body,"Sign out all devices",false,()->new AlertDialog.Builder(activity,android.R.style.Theme_Material_Dialog_Alert).setTitle("Sign out all devices?").setMessage("This revokes all SpotEnhancer sessions when the server receives the request.").setNegativeButton("Cancel",null).setPositiveButton("Sign out",(d,w)->Auth.logout(true)).show()).setEnabled(!Auth.busy());
   EnhancerPanel.label(body,"Premium is assigned by the SpotEnhancer administrator. No payment or subscription purchase is available in this build.",12,EnhancerPanel.MUTED,false);return;
  }
  EditText username=field(activity,body,"Username",false),password=field(activity,body,"Password",true);
  EnhancerPanel.label(body,"Username: 3–32 letters, digits or underscores. Password: 12–128 UTF-8 bytes. Do not use your Spotify password. No email is collected.",12,EnhancerPanel.MUTED,false);
  for(String route:new String[]{"login","register"})EnhancerPanel.button(body,route.equals("login")?"Sign in":"Create free account",route.equals("login"),()->{try{String secret=password.getText().toString();Auth.credentials(route,username.getText().toString().trim(),secret);password.setText("");}catch(RuntimeException error){Toast.makeText(activity,error.getMessage(),Toast.LENGTH_LONG).show();}}).setEnabled(!Auth.busy());
 }
 private static EditText field(Activity a,LinearLayout parent,String label,boolean password){EditText input=new EditText(new ContextThemeWrapper(a,android.R.style.Theme_Material_NoActionBar));input.setSingleLine(true);input.setTextColor(EnhancerPanel.WHITE);input.setHintTextColor(EnhancerPanel.MUTED);input.setHint(label);input.setContentDescription("SpotEnhancer "+label);input.setInputType(password?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);input.setBackground(EnhancerPanel.surface(EnhancerPanel.CARD,12,a,true));input.setPadding(20,10,20,10);parent.addView(input,new LinearLayout.LayoutParams(-1,EnhancerPanel.dp(a,52)));return input;}
 static void locked(Activity a,LinearLayout body,String title){EnhancerPanel.label(body,"🔒 "+title,24,EnhancerPanel.WHITE,true);EnhancerPanel.label(body,"Requires SpotEnhancer Premium. Your current plan is Free. Sign in and refresh membership if your account has been upgraded.",14,EnhancerPanel.MUTED,false);EnhancerPanel.button(body,"Open account",true,()->a.startActivity(new android.content.Intent(a,SpotEnhancerActivity.class).putExtra("screen","account").putExtra("from_spotify",true)));}
 private AccountPanel(){}
}
