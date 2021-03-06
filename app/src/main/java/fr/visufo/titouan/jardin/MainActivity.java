package fr.visufo.titouan.jardin;

import android.animation.LayoutTransition;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import fr.visufo.titouan.jardin.PlantManagement.AddPlantDialogClass;
import fr.visufo.titouan.jardin.PlantManagement.Plant;
import fr.visufo.titouan.jardin.PlantManagement.PlantView;
import fr.visufo.titouan.jardin.Utils.FontsUtils;
import fr.visufo.titouan.jardin.Utils.Randomizer;
import fr.visufo.titouan.jardin.Weather.IResult;
import fr.visufo.titouan.jardin.Weather.MapsActivity;
import fr.visufo.titouan.jardin.Weather.WeatherClass;


public class MainActivity extends AppCompatActivity {

    static final int RESULT_LOAD_IMG = 1;
    private static final int ERROR_DIALOG_REQUEST = 9001;
    static double temp = 0.0;
    //Views
    SpeedDialView mSpeedDialView;

    //AddPlantDialog
    EditText plantEdit;
    EditText degreeEdit;
    Button addImageButton;
    Button addPlantDoneButton;
    Switch aSwitch;
    LinearLayout plantsView;
    Button firstPlantButton;
    TextView tempText;
    LinearLayout mainLinearLayout;

    //Variables
    Bitmap selectedImage;
    String plantName;
    String degree;

    //Actions au lancement de l'application
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Attribuer le fichier activity_main comme Layout de notre activité principale
        setContentView(R.layout.activity_main);
        //Créer le bouton flotant (Floating Action Button)
        addFab();
        //Charge les plantes enregistrées au démarrage de l'application

        String data = readFromFile(getApplicationContext(), "Localisation.latLng");
        if (!(data.isEmpty())) {
            String[] latLgn;
            latLgn = data.split(";");
                Log.v("Latitude", latLgn[0]);
                Log.v("Longitude", latLgn[1]);
                double latitude = Double.parseDouble(latLgn[0]);
                double longitude = Double.parseDouble(latLgn[1]);

                WeatherClass.getTemp(latitude, longitude, new IResult() {
                    @Override
                    public void onResult(double temp) {
                        MainActivity.temp = temp;
                        loadPlants(temp);
                        Log.v("Load", temp + "");
                    }
                }, getApplicationContext());
        }else {
            MainActivity.temp = 100000;
            loadPlants(100000);
            showNextDayTemp(100000);

        }

        String caller = getIntent().getStringExtra("caller");
        if (caller != null) {
            showAddPlantDialog();
        }

        mainLinearLayout = findViewById(R.id.mainLinearLayout);
        LayoutTransition transition = new LayoutTransition();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            transition.enableTransitionType(LayoutTransition.CHANGING);
        }
        transition.setDuration(300);
        mainLinearLayout.setLayoutTransition(transition);


    }

    /*********************
     * FONCTIONS
     * *******************/

    //Fonction permettant de créer le bouton flottant
    public void addFab() {

        //On récupère l'id du bouton flottant dans le fichier activity_main
        mSpeedDialView = findViewById(R.id.speedDial);

        //Ajout du premier sous-bouton "Ajouter une plante" en lui donnant une id, une icone, une couleur pour le fond de l'icone ainsi qu'un label
        mSpeedDialView.addActionItem(new SpeedDialActionItem.Builder(R.id.fab_add_plant, R.drawable.ic_plants)
                .setFabBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.colorAccent, getTheme()))
                .setLabel("Ajouter une plante")
                .create());

        //Ajout du deuxième sous-bouton "Paramètres" en lui donnant une id, une icone, une couleur pour le fond de l'icone ainsi qu'un label
        mSpeedDialView.addActionItem(new SpeedDialActionItem.Builder(R.id.fab_settings, R.drawable.ic_settings)
                .setFabBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.colorAccent, getTheme()))
                .setLabel("Localisation")
                .create());

        //Lecture des actions appliquées au bouton flottant
        mSpeedDialView.setOnActionSelectedListener(new SpeedDialView.OnActionSelectedListener() {
            @Override
            public boolean onActionSelected(SpeedDialActionItem actionItem) {

                //On regarde si l'id de chaque sous-bouton est égale au premier bouton ou au deuxième
                switch (actionItem.getId()) {

                    //Dans le cas ou il s'agit du premier bouton
                    case R.id.fab_add_plant:

                        //On créé une fenêtre de dialogue de type "AddPlant"
                        showAddPlantDialog();
                        mSpeedDialView.close(); //Fermeture du bouton flottant
                        return true;
                    //Dans le cas ou il s'agit du deuxième bouton
                    case R.id.fab_settings:
                        //On créé une fenêtre de dialogue de type "Settings"

                        if (isServicesOK()) {
                            finish();
                            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                            startActivity(intent);

                        }


                        mSpeedDialView.close(); //Fermeture du bouton flottant
                        return true;
                    default:
                        break;
                }
                return true;
            }
        });
    }

    //Fonction ajoutant une plante à la vue d'utilisateur en fonction des différents paramètres
    public void addPlantView(Context context, String name, String degree, boolean isMovable) {
        if (firstPlantButton != null) {
            plantsView.removeView(firstPlantButton);
        }
        saveToInternalStorage(selectedImage, name);
        selectedImage = null;
        Plant plant = new Plant(getApplicationContext(), name, degree, isMovable);
        LinearLayout contentMain = findViewById(R.id.mainLinearLayout);
        PlantView plantView = new PlantView(context, null);
        plantView.setName(name);
        plantView.setDegree(degree);
        if (isNetworkAvailable()) {
            if (temp == 100000) {
                plantView.setInfo("Vous n'avez pas encore indiqué de localisation");
            } else if (temp == -1000000) {
                plantView.setInfo("Problème lié au chargement de la météo");
            } else if (temp <= Double.parseDouble(degree) + 2) {
                if (isMovable) {
                    plantView.setInfo("Pensez à rentrer votre plante");
                    plantView.changeBackgroundColor("#ff7961");
                    plantView.changeTextColor("#131313");
                } else {
                    plantView.setInfo("Pensez à couvrir votre plante");
                    plantView.changeBackgroundColor("#ff7961");
                    plantView.changeTextColor("#131313");
                }
            } else if (temp > Double.parseDouble(degree) + 2) {
                plantView.setInfo("Pas de problème pour cette plante");
            } else {
                plantView.setInfo("Problème lié au chargement de la météo");
            }
        } else if (!isNetworkAvailable()) {
            plantView.setInfo("Pas d'accès internet");
        }
        showImageFromStorage(plantView, name);
        contentMain.addView(plantView);
        showNextDayTemp(temp);
    }

    //Fonction utilisée pour charger les plantes au démarrage de l'application
    public void loadPlants(double temp) {
        Log.v("LOAD PLANT", "Chargement des plantes");
        File[] files = listTxt();
        if (files.length != 0) {
            for (File file : files) {
                String fileName = file.getName();
                String fileContent = readFromFile(getApplicationContext(), fileName);
                String[] plantAttributs;
                plantAttributs = fileContent.split(";");
                String plantName = plantAttributs[0];
                String degree = plantAttributs[1];
                boolean isMovable = Boolean.valueOf(plantAttributs[2]);
                LinearLayout contentMain = findViewById(R.id.mainLinearLayout);
                PlantView plantView = new PlantView(getApplicationContext(), null);
                plantView.setName(plantName);
                plantView.setDegree(degree);
                if (isNetworkAvailable()) {
                    if (temp == 100000) {
                        plantView.setInfo("Vous n'avez pas encore indiqué de localisation");
                    } else if (temp == -1000000) {
                        plantView.setInfo("Problème lié au chargement de la météo");
                    } else if (temp == 0.0) {
                        plantView.setInfo("Problème lié au chargement de la météo");
                    } else if (temp <= Double.parseDouble(degree) + 2) {
                        if (isMovable) {
                            plantView.setInfo("Pensez à rentrer votre plante");
                            plantView.changeBackgroundColor("#ff7961");
                            plantView.changeTextColor("#131313");
                        } else {
                            plantView.setInfo("Pensez à couvrir votre plante");
                            plantView.changeBackgroundColor("#ff7961");
                            plantView.changeTextColor("#131313");
                        }
                    } else if (temp > Double.parseDouble(degree) + 2) {
                        plantView.setInfo("Pas de problème pour cette plante");
                    } else {
                        plantView.setInfo("Problème lié au chargement de la météo");
                    }
                } else if (!isNetworkAvailable()) {
                    plantView.setInfo("Pas d'accès internet");
                }
                Log.v("Plantes:", plantName + ": " + degree + "°C " + "Déplaçable : " + isMovable);
                showImageFromStorage(plantView, plantName);
                contentMain.addView(plantView);
            }
        } else {
            plantsView = findViewById(R.id.mainLinearLayout);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(25, 0, 25, 0);
            firstPlantButton = new Button(this);
            firstPlantButton.setLayoutParams(params);
            firstPlantButton.setText(R.string.firstPlantText);
            firstPlantButton.setAllCaps(false);
            firstPlantButton.setBackgroundResource(R.drawable.ripple_bg_shape);
            firstPlantButton.setTypeface(FontsUtils.getRalewayRegular(getApplicationContext()));
            firstPlantButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddPlantDialog();
                }
            });
            plantsView.addView(firstPlantButton);
        }
        showNextDayTemp(temp);
    }

    public void showAddPlantDialog() {
        final AddPlantDialogClass addPlantDialog = new AddPlantDialogClass(MainActivity.this);
        addPlantDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        //On affiche cette fenêtre de dialogue
        addPlantDialog.show();

        //On récupère l'id du bouton "Valider" de la fenêtre de dialogue que l'on vient de créer
        addPlantDoneButton = addPlantDialog.findViewById(R.id.done_button_addPlant);

        //On récupère l'id du bouton "Ajouter une image" de la fenêtre de dialogue que l'on vient de créer
        addImageButton = addPlantDialog.findViewById(R.id.addImage);

        //Lecture des actions appliquées au bouton "Ajouter une image"
        addImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Création de la requête Android permettant la sélection d'une image
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");

                //Lancement de la requête à l'aide d'une fonction définie à la fin
                startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
            }
        });
        //Lecture des actions appliquées au bouton "Valider"
        addPlantDoneButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                //On récupère l'id du champ de texte correspondant au nom de la plante
                plantEdit = addPlantDialog.findViewById(R.id.plant_name);

                //On récupère l'id du champ de texte correspondant au degré de la plante
                degreeEdit = addPlantDialog.findViewById(R.id.degree_nbr);

                //On récupère l'id de "l'interrupteur" indiquant si la plante est déplaçable ou non
                aSwitch = addPlantDialog.findViewById(R.id.moveable_plant);

                //Définition d'un boolean récupérant l'état de l'interrupteur
                boolean switchState = aSwitch.isChecked();

                //On récupère le texte entré dans le premier champ dans une variable de type String
                plantName = plantEdit.getText().toString().trim();

                //On récupère le texte entré dans le second champ dans une variable de type String
                degree = degreeEdit.getText().toString().trim();

                //Sécurités vérifiant si les champs sont bien remplis

                //Si les variables plantName et degree sont vides, donc si les deux champs ne sont pas remplis, on en informe l'utilisateur
                if (plantName.isEmpty() && degree.isEmpty()) {
                    showToast("Les champs ne sont pas remplis");
                    //Sinon si seulement la variable degree est vide, donc si le 2e champs n'est pas rempli, on en informe l'utilisateur
                } else if (degree.isEmpty()) {
                    showToast("Indiquer un degré de gel");
                    //Sinon si seulement la variable plantName est vide, donc si le 1er champs n'est pas rempli, on en informe l'utilisateur
                } else if (plantName.isEmpty()) {
                    showToast("Indiquer un nom de plante");
                    //Sinon si il n'y a pas d'image de sélectionnée, on en informe l'utilisateur
                } else if (selectedImage == null) {
                    int alea = Randomizer.generate(1, 20);
                    int id = getApplicationContext().getResources().getIdentifier("plant_img_type" + alea, "drawable", getApplicationContext().getPackageName());
                    selectedImage = BitmapFactory.decodeResource(getApplicationContext().getResources(), id);
                    if (switchState) {
                        addPlantView(getApplicationContext(), plantName, degree, true);
                        //On ferme la fenêtre de dialogue
                        addPlantDialog.dismiss();
                        //Sinon, donc si elle ne l'est pas
                    } else {
                        addPlantView(getApplicationContext(), plantName, degree, false);
                        addPlantDialog.dismiss();
                    }
                    //Et finalement si toutes les conditions sont remplies, on ajoute les plantes à la vue principale.
                } else {
                    //Si la plante est déplaçable:
                    if (switchState) {
                        addPlantView(getApplicationContext(), plantName, degree, true);
                        //On ferme la fenêtre de dialogue
                        addPlantDialog.dismiss();
                        //Sinon, donc si elle ne l'est pas
                    } else {
                        addPlantView(getApplicationContext(), plantName, degree, false);
                        addPlantDialog.dismiss();
                    }
                }
            }
        });
    }
    //Fonction permettant de retourner le contenu d'un fichier
    private String readFromFile(Context context, String fileName) {
        //Défnition d'une variable qui retournera le contenu du fichier
        String ret = "";
        //On récupère le contenu du fichier ligne par ligne dans la variable définie plus haut
        try {
            InputStream inputStream = context.openFileInput(fileName);
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }
                inputStream.close();
                ret = stringBuilder.toString();
            }
            //si jamais le fichier n'existe pas
        } catch (FileNotFoundException e) {
            Log.e("File Error", "Fichier non trouvé: " + e.toString());
            //si jamais il y a une autre erreur
        } catch (IOException e) {
            Log.e("F", "Impossible de lire le fichier: " + e.toString());
        }
        //On retourne la variable définie au début, donc le contenu du fichier
        return ret;
    }
    //Fonction propre à Android permettant de récupérer le résultat d'une requête, ici notre requête de sélection d'une image
    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        //Si l'utilisateur a bien sélectionné une image
        if (resultCode == RESULT_OK) {
            try {
                //On récupère l'image sélectionné dans une variable
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                selectedImage = BitmapFactory.decodeStream(imageStream);
                //On change le fond du bouton par l'image sélectionnée
                Drawable plantImg = new BitmapDrawable(getResources(), selectedImage);
                addImageButton.setBackgroundDrawable(plantImg);
                //en cas d'erreur...
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Une erreur s'est produite", Toast.LENGTH_LONG).show();
            }
            //en cas de non sélection d'image
        } else {
            Toast.makeText(getApplicationContext(), "Vous n'avez pas choisi d'image", Toast.LENGTH_LONG).show();
        }
    }
    //Fonction permettant de sauvegarder une image au nom d'une plante
    private void saveToInternalStorage(Bitmap bitmapImage, String plantName) {
        //On récupère le dossier de fichier
        File directory = getApplicationContext().getFilesDir();
        //On créer une variable de type File avec comme chemin le "Fichiers de l'application/nomDeLaPlante.jpg"
        File mypath = new File(directory, plantName + ".jpg");
        //Enregistrement de l'image
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            //On recadre l'image à l'aide d'une fonction, avant de l'enregistrer pour pas prendre trop de place sur le téléphone et pour réduire le temps de chargement
            bitmapImage = getResizedBitmap(bitmapImage, 200);
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            Toast.makeText(getApplicationContext(), "Fichier enregistré " + plantName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //Fonction permettant d'afficher une image au nom d'une plante depuis le stockage
    private void showImageFromStorage(PlantView plantView, String plantName) {
        try {
            String path = getApplicationContext().getFilesDir().toString();

            //On récupère l'image s'appelant "nomPlante.jpg"
            File f = new File(path, plantName + ".jpg");
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            //On ajoute l'image à la PlantView
            plantView.setImage(b);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    //Réduire la taille d'une image, tout en gardant ses proportions
    public Bitmap getResizedBitmap(Bitmap bitmap, int width) {
        float aspectRatio = bitmap.getWidth() /
                (float) bitmap.getHeight();
        int height = Math.round(width / aspectRatio);
        return Bitmap.createScaledBitmap(bitmap, width, height, false);
    }
    public File[] listTxt() {

        File[] files;
        //On récupère le chemin pour accéder aux fichiers de l'application
        String path = getApplicationContext().getFilesDir().toString();

        //On récupère le dossier de fichiers de l'application dans une variable
        File directory = new File(path);

        //Création d'un tableau de fichiers de type .txt
        files = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return (file.getPath().endsWith(".txt"));
            }
        });
        return files;
    }
    public void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
    public boolean isServicesOK() {
        Log.d("Maps", "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            //everything is fine and the user can make map requests
            Log.d("Maps", "isServicesOK: Google Play Services is working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //an error occured but we can resolve it
            Log.d("Maps", "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    public void showNextDayTemp(double temp) {
        mainLinearLayout = findViewById(R.id.mainLinearLayout);
        if (tempText == null) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(10, 10, 10, 10);
            params.gravity = Gravity.CENTER;

            tempText = new TextView(getApplicationContext());
            if (temp == 100000) {
                tempText.setText("Veuillez indiquer une localisation");
            } else if (temp == -100000) {
                tempText.setText("Problème lors du chargement de la météo");
            } else {
                tempText.setText("Température minimale prévue: " + (Math.round(temp * 10.0) / 10.0) + "°C");
            }

            tempText.setTextSize(9);
            tempText.setLayoutParams(params);
            tempText.setTypeface(FontsUtils.getRalewayLight(getApplicationContext()));
            tempText.setTextColor(Color.parseColor("#FFFFFF"));
            mainLinearLayout.addView(tempText);
        } else {
            mainLinearLayout.removeView(tempText);
            tempText = null;
            showNextDayTemp(temp);
        }
    }
}