package org.example.spotifymixer.runtime;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;

/** Resource-free Theme Studio: the installer injects DEX only, not Android resources. */
final class ThemeStudio {
    static final String ROOT_TAG = "spotenhancer-theme-studio";
    private static final int BG=0xff090e0c, CARD=0xff191e1c, LINE=0xff343c38;
    private static final int WHITE=0xfff4f6f5, MUTED=0xffb4bcb8, GREEN=0xff37e583;
    private static final String FAVORITES="theme_favorites";
    private final Activity activity;
    private final SharedPreferences prefs;
    private final Runnable back;
    private final Set<String> favorites;
    private final LinearLayout shell, content;
    private LinearLayout grid, filters, footer;
    private TextView count, selectedName, footerCaption;
    private Button apply, themesTab, settingsTab;
    private Preview footerArtwork;
    private Switch enabledSwitch, blackSwitch;
    private ThemePalette selected;
    private boolean trueBlack, settings, closed, updating;
    private String query="", filter="All";
    private int libraryScroll, settingsScroll;
    private ScrollView libraryViewport, settingsViewport;

    ThemeStudio(Activity activity, SharedPreferences prefs, Runnable back, Bundle state) {
        this.activity=activity; this.prefs=prefs; this.back=back;
        selected=ThemePalette.find(prefs.getString("theme_preset","amoled"));
        trueBlack=prefs.getBoolean("amoled_enabled",false);
        favorites=new HashSet<>(prefs.getStringSet(FAVORITES,Collections.emptySet()));
        if(state!=null) {
            selected=ThemePalette.find(state.getString("studio.preset",selected.id));
            trueBlack=state.getBoolean("studio.black",trueBlack);
            settings=state.getBoolean("studio.settings",false);
            query=state.getString("studio.query","");
            filter=state.getString("studio.filter","All");
            libraryScroll=state.getInt("studio.libraryScroll",0);
            settingsScroll=state.getInt("studio.settingsScroll",0);
        }
        shell=column(); shell.setTag(ROOT_TAG); shell.setBackgroundColor(BG);
        shell.setOnApplyWindowInsetsListener((view,insets)->{
            view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        activity.getWindow().setBackgroundDrawable(new ColorDrawable(BG));
        activity.getWindow().setStatusBarColor(BG);
        activity.getWindow().setNavigationBarColor(BG);
        activity.getWindow().getDecorView().setSystemUiVisibility(0);
        header();
        content=column(); shell.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        buildFooter();
        activity.setContentView(shell); shell.requestApplyInsets();
        renderContent(); refreshAccess();
    }

    private Context context(){return activity;}
    private int dp(float value){return Math.round(value*context().getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout box=new LinearLayout(context());box.setOrientation(LinearLayout.VERTICAL);return box;}
    private LinearLayout row(){LinearLayout box=new LinearLayout(context());box.setGravity(Gravity.CENTER_VERTICAL);return box;}
    private LinearLayout.LayoutParams size(int width,int height){return new LinearLayout.LayoutParams(width<0?width:dp(width),height<0?height:dp(height));}
    private GradientDrawable surface(int color,int radius,int border){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(border!=0)d.setStroke(dp(1),border);return d;}
    private TextView label(String text,int sp,int color,boolean bold){
        TextView view=new TextView(context());view.setText(text);view.setTextSize(sp);view.setTextColor(color);
        view.setIncludeFontPadding(false);if(bold)view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return view;
    }
    private Button button(String title,boolean primary,Runnable action){
        Button view=new Button(new ContextThemeWrapper(context(),android.R.style.Theme_Material_NoActionBar));
        view.setText(title);view.setAllCaps(false);view.setTextSize(13);view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        view.setMinHeight(0);view.setMinimumHeight(0);view.setMinWidth(0);view.setMinimumWidth(0);
        view.setPadding(dp(12),0,dp(12),0);view.setElevation(0);view.setStateListAnimator(null);
        view.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{0xff818c85,primary?0xff071b10:WHITE}));
        StateListDrawable background=new StateListDrawable();
        background.addState(new int[]{-android.R.attr.state_enabled},surface(0xff27322b,12,LINE));
        background.addState(new int[]{},surface(primary?GREEN:CARD,12,primary?0:LINE));
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x25ffffff),background,null));
        view.setOnClickListener(v->{if(!closed)action.run();});return view;
    }
    private ImageButton icon(String kind,String description,int color,Runnable action){
        ImageButton view=new ImageButton(context());view.setImageDrawable(new Icon(kind,color));
        view.setContentDescription(description);view.setPadding(dp(12),dp(12),dp(12),dp(12));
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x25ffffff),surface(Color.TRANSPARENT,24,0),null));
        view.setOnClickListener(v->{if(!closed)action.run();});return view;
    }
    private void header(){
        LinearLayout top=row();top.setPadding(dp(4),dp(10),dp(16),0);
        top.addView(icon("back","Back to SpotEnhancer",WHITE,back),size(48,48));
        top.addView(label("Theme Studio",24,WHITE,true),new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout premium=row();premium.setPadding(dp(8),dp(6),dp(8),dp(6));
        premium.setBackground(surface(Color.TRANSPARENT,20,0xffc3993e));
        ImageView crown=new ImageView(context());crown.setImageDrawable(new Icon("crown",0xffffcf66));premium.addView(crown,size(14,14));
        TextView badge=label(" PREMIUM",9,0xffffcf66,true);premium.addView(badge);top.addView(premium);
        shell.addView(top);
        TextView subtitle=label("Give your music a new look",13,MUTED,false);
        subtitle.setPadding(dp(52),0,dp(16),dp(14));shell.addView(subtitle);
        LinearLayout enable=row();enable.setPadding(dp(14),0,dp(10),0);enable.setBackground(surface(CARD,12,0));
        TextView title=label("Enable themes",14,WHITE,true);enable.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        enabledSwitch=toggle("Enable themes");enabledSwitch.setChecked(prefs.getBoolean("theme_enabled",false));
        enable.addView(enabledSwitch,size(52,48));
        LinearLayout.LayoutParams ep=size(-1,50);ep.setMargins(dp(16),0,dp(16),dp(10));shell.addView(enable,ep);
        enabledSwitch.setOnCheckedChangeListener((view,on)->{
            if(updating||closed)return;
            if(!authorized()){syncEnabled();return;}
            SharedPreferences.Editor edit=prefs.edit().putBoolean("theme_enabled",on);if(!on)edit.putBoolean("amoled_enabled",false);
            if(!save(edit)){syncEnabled();return;}
            rememberScroll();ThemeRuntime.refresh(activity);renderContent();updateFooter();
        });
        LinearLayout tabs=row();tabs.setPadding(dp(3),dp(3),dp(3),dp(3));tabs.setBackground(surface(CARD,12,LINE));
        themesTab=button("Themes",false,()->switchTab(false));settingsTab=button("Settings",false,()->switchTab(true));
        tabs.addView(themesTab,new LinearLayout.LayoutParams(0,dp(40),1));
        tabs.addView(settingsTab,new LinearLayout.LayoutParams(0,dp(40),1));
        LinearLayout.LayoutParams tp=size(-1,46);tp.setMargins(dp(16),0,dp(16),dp(10));shell.addView(tabs,tp);
    }
    private Switch toggle(String description){
        Switch view=new Switch(new ContextThemeWrapper(context(),android.R.style.Theme_Material_NoActionBar));
        view.setContentDescription(description);view.setShowText(false);view.setSplitTrack(false);view.setSwitchMinWidth(dp(40));
        GradientDrawable thumb=surface(WHITE,12,0);thumb.setShape(GradientDrawable.OVAL);thumb.setSize(dp(20),dp(20));view.setThumbDrawable(thumb);
        StateListDrawable track=new StateListDrawable();
        GradientDrawable on=surface(GREEN,14,0);on.setSize(dp(40),dp(24));
        GradientDrawable off=surface(0xff58605c,14,0);off.setSize(dp(40),dp(24));
        track.addState(new int[]{android.R.attr.state_checked},on);track.addState(new int[]{},off);view.setTrackDrawable(track);
        return view;
    }
    private void rememberScroll(){
        if(libraryViewport!=null)libraryScroll=libraryViewport.getScrollY();
        if(settingsViewport!=null)settingsScroll=settingsViewport.getScrollY();
    }
    private void switchTab(boolean next){
        if(next==settings)return;rememberScroll();settings=next;hideKeyboard();renderContent();
    }
    private void hideKeyboard(){InputMethodManager keyboard=(InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.hideSoftInputFromWindow(shell.getWindowToken(),0);shell.clearFocus();}
    private void renderContent(){
        content.removeAllViews();libraryViewport=null;settingsViewport=null;blackSwitch=null;
        themesTab.setSelected(!settings);settingsTab.setSelected(settings);
        themesTab.setBackground(surface(settings?Color.TRANSPARENT:0xff176b48,10,0));
        settingsTab.setBackground(surface(settings?0xff176b48:Color.TRANSPARENT,10,0));
        if(settings)renderSettings();else renderLibrary();
        updateFooter();
    }
    private void renderLibrary(){
        EditText search=new EditText(new ContextThemeWrapper(context(),android.R.style.Theme_Material_NoActionBar));
        search.setSingleLine(true);search.setTextSize(14);search.setTextColor(WHITE);search.setHintTextColor(MUTED);
        search.setHint("Search themes");search.setContentDescription("Search themes");search.setText(query);
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        search.setPadding(dp(14),0,dp(14),0);search.setBackground(surface(CARD,14,LINE));
        Icon searchIcon=new Icon("search",MUTED);searchIcon.setBounds(0,0,dp(18),dp(18));
        search.setCompoundDrawables(searchIcon,null,null,null);search.setCompoundDrawablePadding(dp(10));
        LinearLayout.LayoutParams sp=size(-1,46);sp.setMargins(dp(16),0,dp(16),dp(6));content.addView(search,sp);
        search.setOnEditorActionListener((v,action,event)->{hideKeyboard();return true;});
        HorizontalScrollView categoryScroll=new HorizontalScrollView(context());categoryScroll.setHorizontalScrollBarEnabled(false);
        categoryScroll.setClipToPadding(false);categoryScroll.setPadding(dp(12),0,dp(12),0);
        filters=row();categoryScroll.addView(filters);content.addView(categoryScroll,size(-1,48));renderFilters();
        count=label("",11,MUTED,false);count.setPadding(dp(18),dp(2),dp(18),dp(8));content.addView(count);
        libraryViewport=new ScrollView(context());libraryViewport.setClipToPadding(false);libraryViewport.setPadding(dp(12),0,dp(12),dp(8));
        grid=column();libraryViewport.addView(grid,new ScrollView.LayoutParams(-1,-2));content.addView(libraryViewport,new LinearLayout.LayoutParams(-1,0,1));
        renderGrid();libraryViewport.post(()->{if(!closed&&libraryViewport!=null)libraryViewport.scrollTo(0,libraryScroll);});
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){query=s.toString();libraryScroll=0;renderGrid();if(libraryViewport!=null)libraryViewport.scrollTo(0,0);}
            public void afterTextChanged(Editable text){}
        });
    }
    private void renderFilters(){
        filters.removeAllViews();
        for(String value:new String[]{"All","AMOLED","Cool","Warm","Neon","Favorites"}){
            boolean active=filter.equals(value);Button chip=button(value,false,()->{filter=value;libraryScroll=0;renderFilters();renderGrid();libraryViewport.scrollTo(0,0);});
            chip.setTextSize(12);chip.setTextColor(active?0xff071b10:WHITE);chip.setSelected(active);
            chip.setBackground(surface(active?GREEN:BG,22,active?0:LINE));
            LinearLayout.LayoutParams cp=size(-2,40);cp.setMargins(dp(4),dp(4),dp(4),dp(4));filters.addView(chip,cp);
        }
    }
    private void renderGrid(){
        if(grid==null||settings)return;grid.removeAllViews();
        List<ThemePalette> visible=new ArrayList<>();
        for(ThemePalette p:ThemePalette.PRESETS)if(p.matches(query,"Favorites".equals(filter)?"All":filter)&&(!"Favorites".equals(filter)||favorites.contains(p.id)))visible.add(p);
        count.setText(visible.size()+(visible.size()==1?" theme":" themes")+(filter.equals("All")&&query.trim().isEmpty()?" · Choose your next mood":" · "+filter));
        if(visible.isEmpty()){
            TextView empty=label("Favorites".equals(filter)?"No favorites yet\nTap a heart to save a theme.":"No themes found\nTry another name or filter.",15,MUTED,false);
            empty.setGravity(Gravity.CENTER);empty.setPadding(dp(20),dp(40),dp(20),dp(40));grid.addView(empty);return;
        }
        int columns=context().getResources().getConfiguration().screenWidthDp<340||context().getResources().getConfiguration().fontScale>1.3f?1:2;
        LinearLayout line=null;
        for(int i=0;i<visible.size();i++){
            if(i%columns==0){line=row();line.setGravity(Gravity.TOP);grid.addView(line,size(-1,-2));}
            ThemePalette p=visible.get(i);LinearLayout tile=column();LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(0,-2,1);cell.setMargins(dp(4),0,dp(4),dp(12));line.addView(tile,cell);
            FrameLayout frame=new FrameLayout(context());boolean chosen=p.id.equals(selected.id);
            frame.setBackground(surface(p.background,12,chosen?p.accent:p.line));
            if(chosen)((GradientDrawable)frame.getBackground()).setStroke(dp(2),p.accent);
            frame.setPadding(dp(2),dp(2),dp(2),dp(2));frame.setClipToOutline(true);frame.setSelected(chosen);frame.setContentDescription("Preview "+p.name);
            frame.setFocusable(true);frame.setOnClickListener(v->{selected=p;rememberScroll();renderGrid();updateFooter();});
            Preview preview=new Preview(context(),p,false,false,statusFor(p));frame.addView(preview,new FrameLayout.LayoutParams(-1,-1));
            ImageButton heart=icon(favorites.contains(p.id)?"heart-filled":"heart",(favorites.contains(p.id)?"Remove ":"Favorite ")+p.name+(favorites.contains(p.id)?" from favorites":""),p.text,()->favorite(p));
            FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.TOP|Gravity.RIGHT);frame.addView(heart,hp);
            tile.addView(frame,size(-1,100));TextView name=label(p.name,12,WHITE,false);name.setPadding(dp(2),dp(7),0,0);tile.addView(name);
        }
        if(visible.size()%columns!=0)line.addView(new android.view.View(context()),new LinearLayout.LayoutParams(0,1,1));
    }
    private String statusFor(ThemePalette p){
        if(prefs.getBoolean("theme_enabled",false)&&p.id.equals(prefs.getString("theme_preset","amoled")))return "Active";
        return p.id.equals(selected.id)?"Preview":"";
    }
    private void favorite(ThemePalette p){
        Set<String> next=new HashSet<>(favorites);if(!next.add(p.id))next.remove(p.id);
        if(save(prefs.edit().putStringSet(FAVORITES,next))){favorites.clear();favorites.addAll(next);rememberScroll();renderGrid();}
    }
    private void renderSettings(){
        settingsViewport=new ScrollView(context());settingsViewport.setClipToPadding(false);settingsViewport.setPadding(dp(16),dp(2),dp(16),dp(12));
        LinearLayout body=column();settingsViewport.addView(body,new ScrollView.LayoutParams(-1,-2));content.addView(settingsViewport,new LinearLayout.LayoutParams(-1,0,1));
        Preview preview=new Preview(context(),selected,true,trueBlack,isApplied()?"Active":"Preview");
        preview.setContentDescription(selected.name+" theme preview. Illustrative artwork and player controls.");
        FrameLayout previewFrame=new FrameLayout(context());previewFrame.setBackground(surface(trueBlack?Color.BLACK:selected.background,16,selected.line));previewFrame.setClipToOutline(true);previewFrame.setPadding(dp(1),dp(1),dp(1),dp(1));previewFrame.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        ImageButton favorite=icon(favorites.contains(selected.id)?"heart-filled":"heart",(favorites.contains(selected.id)?"Remove ":"Favorite ")+selected.name+(favorites.contains(selected.id)?" from favorites":""),selected.text,()->{favorite(selected);rememberScroll();renderContent();});
        previewFrame.addView(favorite,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.TOP|Gravity.RIGHT));body.addView(previewFrame,size(-1,210));
        LinearLayout swatches=row();swatches.setGravity(Gravity.TOP);String[] names={"Background","Surface","Accent","Text","Subtext"};
        int[] colors={trueBlack?Color.BLACK:selected.background,selected.surface,selected.accent,selected.text,selected.muted};
        for(int i=0;i<colors.length;i++){
            LinearLayout group=column();group.setGravity(Gravity.CENTER_HORIZONTAL);group.setPadding(0,dp(14),0,dp(16));
            View dot=new View(context());GradientDrawable circle=surface(colors[i],30,0xff66636c);circle.setShape(GradientDrawable.OVAL);dot.setBackground(circle);group.addView(dot,size(30,30));
            TextView name=label(names[i],10,MUTED,false);name.setGravity(Gravity.CENTER);name.setPadding(0,dp(6),0,0);group.addView(name);
            group.setContentDescription(names[i]+" "+String.format(Locale.ROOT,"#%06X",colors[i]&0xffffff));
            swatches.addView(group,new LinearLayout.LayoutParams(0,-2,1));
        }body.addView(swatches);
        LinearLayout appearance=column();appearance.setPadding(dp(14),dp(14),dp(12),dp(6));appearance.setBackground(surface(CARD,14,LINE));
        TextView heading=label("Appearance",16,WHITE,true);appearance.addView(heading);
        View divider=new View(context());divider.setBackgroundColor(LINE);LinearLayout.LayoutParams dl=size(-1,1);dl.setMargins(0,dp(12),0,dp(6));appearance.addView(divider,dl);
        LinearLayout blackRow=row();LinearLayout copy=column();copy.addView(label("True black",14,WHITE,true));TextView help=label("Black backgrounds for AMOLED",11,MUTED,false);help.setPadding(0,dp(5),0,0);copy.addView(help);blackRow.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        blackSwitch=toggle("True black");blackSwitch.setChecked(trueBlack);blackRow.addView(blackSwitch,size(52,52));appearance.addView(blackRow);body.addView(appearance);
        blackSwitch.setOnCheckedChangeListener((view,on)->{if(updating||closed)return;trueBlack=on;rememberScroll();renderContent();});
        TextView note=label("Your preview is only saved when you tap Apply.",11,MUTED,false);note.setPadding(0,dp(14),0,dp(12));body.addView(note);
        LinearLayout actions=row();Button reopen=button("Refresh screen",false,()->{if(authorized())ThemeRuntime.reopen(activity);});Button restore=button("Restore defaults",false,this::confirmRestore);
        Icon refreshIcon=new Icon("refresh",WHITE);refreshIcon.setBounds(0,0,dp(16),dp(16));reopen.setCompoundDrawables(refreshIcon,null,null,null);reopen.setCompoundDrawablePadding(dp(5));reopen.setTextSize(11);
        Icon resetIcon=new Icon("reset",WHITE);resetIcon.setBounds(0,0,dp(16),dp(16));restore.setCompoundDrawables(resetIcon,null,null,null);restore.setCompoundDrawablePadding(dp(5));restore.setTextSize(11);
        actions.addView(reopen,new LinearLayout.LayoutParams(0,dp(46),1));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,dp(46),1);rp.leftMargin=dp(8);actions.addView(restore,rp);body.addView(actions);
        TextView hint=label("Some screens need to be reopened to update.",10,MUTED,false);hint.setPadding(0,dp(8),0,dp(12));body.addView(hint);
        TextView limits=label("Themes change supported interface colors. Album artwork, videos, fonts and layouts stay original.",11,MUTED,false);limits.setLineSpacing(dp(3),1);body.addView(limits);
        settingsViewport.post(()->{if(!closed&&settingsViewport!=null)settingsViewport.scrollTo(0,settingsScroll);});
    }
    private void buildFooter(){
        footer=row();footer.setPadding(dp(16),dp(12),dp(16),dp(14));footer.setBackground(surface(0xff181e1b,16,LINE));
        footerArtwork=new Preview(context(),selected,false,false,"");footerArtwork.artOnly=true;LinearLayout.LayoutParams ap=size(36,36);ap.rightMargin=dp(12);footer.addView(footerArtwork,ap);
        LinearLayout copy=column();footerCaption=label("Selected",11,MUTED,false);selectedName=label(selected.name,14,WHITE,true);selectedName.setPadding(0,dp(4),dp(8),0);copy.addView(footerCaption);copy.addView(selectedName);footer.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        apply=button("Apply",true,this::apply);footer.addView(apply,size(116,48));shell.addView(footer,size(-1,-2));
    }
    private boolean isApplied(){return prefs.getBoolean("theme_enabled",false)&&selected.id.equals(prefs.getString("theme_preset","amoled"))&&trueBlack==prefs.getBoolean("amoled_enabled",false);}
    private void updateFooter(){
        if(apply==null)return;footerArtwork.palette=selected;footerArtwork.invalidate();selectedName.setText(selected.name);footerCaption.setText(isApplied()?"Active":"Selected");
        apply.setText("Apply");apply.setEnabled(Auth.allowed("themes")&&prefs.getBoolean("theme_enabled",false));
        if(blackSwitch!=null)blackSwitch.setEnabled(Auth.allowed("themes"));
    }
    private void apply(){
        if(!authorized()||!prefs.getBoolean("theme_enabled",false))return;
        if(!save(prefs.edit().putString("theme_preset",selected.id).putBoolean("theme_enabled",true).putBoolean("amoled_enabled",trueBlack)))return;
        rememberScroll();ThemeRuntime.refresh(activity);renderContent();
        Toast.makeText(activity,selected.name+" applied",Toast.LENGTH_SHORT).show();
    }
    private void confirmRestore(){
        new AlertDialog.Builder(activity,android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Restore Spotify defaults?")
            .setMessage("Turn off themes and True black. Your favorites and other settings will be kept.")
            .setNegativeButton("Cancel",null).setPositiveButton("Restore",(dialog,which)->{
                if(closed||!authorized())return;
                if(!save(prefs.edit().putBoolean("theme_enabled",false).putBoolean("amoled_enabled",false).remove("theme_preset")))return;
                selected=ThemePalette.find("amoled");trueBlack=false;syncEnabled();ThemeRuntime.refresh(activity);rememberScroll();renderContent();
                Toast.makeText(activity,"Default colors restored",Toast.LENGTH_SHORT).show();
            }).show();
    }
    private boolean authorized(){if(Auth.allowed("themes"))return true;Toast.makeText(activity,"SpotEnhancer Premium is required.",Toast.LENGTH_LONG).show();return false;}
    private boolean save(SharedPreferences.Editor edit){if(edit.commit())return true;Toast.makeText(activity,"Could not save your settings. Try again.",Toast.LENGTH_LONG).show();return false;}
    private void syncEnabled(){updating=true;enabledSwitch.setChecked(prefs.getBoolean("theme_enabled",false));updating=false;}
    void refreshAccess(){if(closed)return;enabledSwitch.setEnabled(Auth.allowed("themes"));syncEnabled();updateFooter();}
    void saveState(Bundle out){rememberScroll();out.putString("studio.preset",selected.id);out.putBoolean("studio.black",trueBlack);out.putBoolean("studio.settings",settings);out.putString("studio.query",query);out.putString("studio.filter",filter);out.putInt("studio.libraryScroll",libraryScroll);out.putInt("studio.settingsScroll",settingsScroll);}
    void close(){closed=true;hideKeyboard();}

    /** Self-contained illustrative music previews; no artwork downloads or resources. */
    private static final class Preview extends View {
        private ThemePalette palette;
        private boolean artOnly;
        private final boolean large, black;
        private final String status;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        Preview(Context context,ThemePalette p,boolean large,boolean black,String status){super(context);palette=p;this.large=large;this.black=black;this.status=status;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        protected void onDraw(Canvas canvas){
            super.onDraw(canvas);if(artOnly){art(canvas,0,0,Math.min(getWidth(),getHeight()));return;}canvas.save();canvas.scale(getWidth()/(large?360f:180f),getHeight()/(large?210f:100f));
            int bg=black?Color.BLACK:palette.background;
            paint.setColor(Color.WHITE);paint.setShader(new LinearGradient(0,0,large?360:180,large?210:100,palette.surface,bg,Shader.TileMode.CLAMP));
            canvas.drawRoundRect(new RectF(0,0,large?360:180,large?210:100),12,12,paint);paint.setShader(null);
            if(large){
                text(canvas,palette.name,16,25,15,palette.text,true);
                pill(canvas,status,230,12,palette.muted);
                art(canvas,16,46,92);
                text(canvas,"Late night radio",124,79,15,palette.text,true);
                text(canvas,"SpotEnhancer",124,101,12,palette.muted,false);
                line(canvas,124,125,342,125,palette.line,3);line(canvas,124,125,202,125,palette.accent,3);
                text(canvas,"1:28",124,143,9,palette.muted,false);text(canvas,"3:56",320,143,9,palette.muted,false);
                glyph(canvas,"shuffle",132,162,18,palette.muted);glyph(canvas,"previous",174,162,18,palette.text);
                circle(canvas,224,173,23,palette.accent);glyph(canvas,"play",213,162,22,0xff07150c);
                glyph(canvas,"next",270,162,18,palette.text);glyph(canvas,"repeat",313,162,18,palette.muted);
            }else{
                if(!status.isEmpty())pill(canvas,status,10,8,palette.accent);
                if("Preview".equals(status)){circle(canvas,126,17,8,palette.accent);glyph(canvas,"check",120,11,12,palette.background);}
                art(canvas,10,34,40);
                text(canvas,"Late night radio",60,50,9,palette.text,true);
                text(canvas,"SpotEnhancer",60,65,8,palette.muted,false);
                line(canvas,10,86,135,86,palette.line,3);line(canvas,10,86,67,86,palette.accent,3);
                circle(canvas,158,86,11,palette.accent);glyph(canvas,"play",151,79,14,0xff07150c);
            }canvas.restore();
        }
        private void text(Canvas canvas,String value,float x,float y,int sp,int color,boolean bold){paint.setColor(color);paint.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextSize(sp);canvas.drawText(value,x,y,paint);}
        private void pill(Canvas canvas,String value,float x,float y,int color){paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(9);float width=paint.measureText(value)+16;paint.setColor(palette.surface);canvas.drawRoundRect(new RectF(x,y,x+width,y+18),9,9,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.7f);paint.setColor(color);canvas.drawRoundRect(new RectF(x,y,x+width,y+18),9,9,paint);paint.setStyle(Paint.Style.FILL);text(canvas,value,x+8,y+12,9,color,false);}
        private void circle(Canvas c,float x,float y,float radius,int color){paint.setColor(color);c.drawCircle(x,y,radius,paint);}
        private void line(Canvas c,float x,float y,float endX,float endY,int color,float width){paint.setColor(color);paint.setStrokeWidth(width);paint.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,endX,endY,paint);}
        private void glyph(Canvas c,String name,int x,int y,int size,int color){Icon icon=new Icon(name,color);icon.setBounds(x,y,x+size,y+size);icon.draw(c);}
        private void art(Canvas canvas,float x,float y,float size){
            canvas.save();canvas.translate(x,y);canvas.scale(size/100,size/100);
            Path clip=new Path();clip.addRoundRect(new RectF(0,0,100,100),6,6,Path.Direction.CW);canvas.clipPath(clip);
            // Fixed original illustration per theme: True black never changes these pixels.
            paint.setColor(Color.WHITE);paint.setShader(new LinearGradient(0,0,100,100,palette.accent,palette.background,Shader.TileMode.CLAMP));canvas.drawRect(0,0,100,100,paint);paint.setShader(null);
            int variant=ThemePalette.PRESETS.indexOf(palette)%3;
            if(variant==0){
                paint.setColor(0x66203933);canvas.drawOval(new RectF(-20,20,65,120),paint);paint.setColor(0x884ffff0);canvas.drawOval(new RectF(50,-20,105,80),paint);
                line(canvas,15,100,65,12,0x88ffffff,1.2f);for(int i=0;i<5;i++)line(canvas,26+i*7,81-i*13,14+i*5,56-i*8,0x66ffffff,1);
            }else if(variant==1){
                paint.setColor(0x88ffffff);canvas.drawCircle(76,20,12,paint);Path mountain=new Path();mountain.moveTo(-5,90);mountain.lineTo(42,26);mountain.lineTo(110,100);mountain.close();paint.setColor(palette.surface);canvas.drawPath(mountain,paint);mountain.reset();mountain.moveTo(25,100);mountain.lineTo(78,46);mountain.lineTo(118,100);mountain.close();paint.setColor(0x99344959);canvas.drawPath(mountain,paint);
            }else{
                paint.setShader(new RadialGradient(40,28,55,new int[]{0xfff2d8ff,palette.accent,palette.surface},null,Shader.TileMode.CLAMP));canvas.drawCircle(34,37,34,paint);paint.setShader(new RadialGradient(65,65,55,new int[]{palette.accent,palette.surface,palette.background},null,Shader.TileMode.CLAMP));canvas.drawCircle(76,77,40,paint);paint.setShader(null);
            }
            canvas.restore();
        }
    }

    /** Crisp original vector icons; kept inside DEX so patched Spotify needs no R resources. */
    private static final class Icon extends Drawable {
        private final String kind;private final int color;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        Icon(String kind,int color){this.kind=kind;this.color=color;}
        public void draw(Canvas canvas){
            canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/24f,getBounds().height()/24f);
            paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.6f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
            Path p=new Path();
            if(kind.startsWith("heart")){p.moveTo(12,21);p.cubicTo(10,18,2,13,2,7);p.cubicTo(2,1,10,1,12,6);p.cubicTo(14,1,22,1,22,7);p.cubicTo(22,13,14,18,12,21);if(kind.equals("heart-filled"))paint.setStyle(Paint.Style.FILL);canvas.drawPath(p,paint);}
            else if(kind.equals("back")){canvas.drawLine(20,12,4,12,paint);canvas.drawLine(4,12,10,6,paint);canvas.drawLine(4,12,10,18,paint);}
            else if(kind.equals("search")){canvas.drawCircle(10,10,6,paint);canvas.drawLine(15,15,21,21,paint);}
            else if(kind.equals("crown")){p.moveTo(3,6);p.lineTo(7,11);p.lineTo(12,3);p.lineTo(17,11);p.lineTo(21,6);p.lineTo(19,19);p.lineTo(5,19);p.close();paint.setStyle(Paint.Style.FILL);canvas.drawPath(p,paint);}
            else if(kind.equals("refresh")){canvas.drawArc(new RectF(4,4,20,20),-50,285,false,paint);canvas.drawLine(20,3,20,9,paint);canvas.drawLine(20,9,14,9,paint);}
            else if(kind.equals("reset")){canvas.drawArc(new RectF(4,4,20,20),-140,285,false,paint);canvas.drawLine(3,3,3,9,paint);canvas.drawLine(3,9,9,9,paint);}
            else if(kind.equals("check")){canvas.drawLine(5,12,10,17,paint);canvas.drawLine(10,17,20,6,paint);}
            else if(kind.equals("play")){p.moveTo(8,4);p.lineTo(20,12);p.lineTo(8,20);p.close();paint.setStyle(Paint.Style.FILL);canvas.drawPath(p,paint);}
            else if(kind.equals("previous")||kind.equals("next")){boolean prev=kind.equals("previous");p.moveTo(prev?18:6,5);p.lineTo(prev?7:17,12);p.lineTo(prev?18:6,19);p.close();paint.setStyle(Paint.Style.FILL);canvas.drawPath(p,paint);paint.setStrokeWidth(2);canvas.drawLine(prev?5:19,5,prev?5:19,19,paint);}
            else if(kind.equals("shuffle")){canvas.drawLine(3,5,7,5,paint);canvas.drawLine(7,5,17,19,paint);canvas.drawLine(17,19,22,19,paint);canvas.drawLine(19,16,22,19,paint);canvas.drawLine(3,19,7,19,paint);canvas.drawLine(7,19,17,5,paint);canvas.drawLine(17,5,22,5,paint);canvas.drawLine(19,2,22,5,paint);}
            else{canvas.drawRoundRect(new RectF(3,6,21,18),4,4,paint);canvas.drawLine(17,3,21,6,paint);canvas.drawLine(3,18,7,21,paint);}
            canvas.restore();
        }
        public void setAlpha(int alpha){paint.setAlpha(alpha);}public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
