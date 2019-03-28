package fr.visufo.titouan.jardin;

import android.content.Context;
import android.content.res.TypedArray;


import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;


public class PlantView extends FrameLayout {

    //Views
    private UnderlinedTextView nameView;
    private TextView infoView;
    private TextView degreeView;
    private CircleImageView imageView;

    //Attributes
    private String nameText;
    private String infoText;
    private String degreeText;
    private Drawable plantImage;


    /**
     * Constructor.
     *
     * @param context the context.
     */
    public PlantView(@NonNull Context context) {
        super(context);
        obtainStyledAttributes(context, null, 0);
        init();
    }

    /**
     * Constructor.
     *
     * @param context the context.
     * @param attrs   the attributes from the layout.
     */
    public PlantView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        obtainStyledAttributes(context, attrs, 0);
        init();
    }

    /**
     * Constructor.
     *
     * @param context      the context.
     * @param attrs        the attributes from the layout.
     * @param defStyleAttr the attributes from the default style.
     */
    public PlantView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        obtainStyledAttributes(context, attrs, defStyleAttr);
        init();
    }

    private void obtainStyledAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        if (attrs != null) {
            TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PlantView, defStyleAttr, 0);
            nameText = typedArray.getString(R.styleable.PlantView_name);
            infoText = typedArray.getString(R.styleable.PlantView_info);
            degreeText = typedArray.getString(R.styleable.PlantView_degree);
            plantImage = typedArray.getDrawable(R.styleable.PlantView_android_src);

        }
    }

    private void init() {
        inflate(getContext(), R.layout.plantview, this);
        nameView = findViewById(R.id.nomPlante);
        infoView = findViewById(R.id.info);
        degreeView = findViewById(R.id.degree);
        imageView = findViewById(R.id.image);
        setupView();
    }

    private void setupView() {

        nameView.setText(nameText);
        infoView.setText(infoText);
        degreeView.setText(degreeText +" °C");
        imageView.setImageDrawable(plantImage);
    }

    public void setName(String name) {
        nameView.setText(name);
    }
    public void setInfo(String info){
        infoView.setText(info);
    }
    public void setDegree(String degree){
        degreeView.setText(degree+" °C");
    }
    public void setImage(Bitmap image){
        imageView.setImageBitmap(image);
    }
}