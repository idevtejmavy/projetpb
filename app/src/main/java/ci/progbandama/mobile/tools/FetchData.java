package ci.progbandama.mobile.tools;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.List;
import java.net.MalformedURLException;
import java.net.URL;

import ci.progbandama.mobile.models.MissVersion;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class FetchData {

    public static class AppVersion extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<Context> contextRef;
        private LibraryListener listener;
        private String link;
        private MissVersion mVers;

        public AppVersion(Context context, String link, LibraryListener listener) {
            this.contextRef = new WeakReference<>(context);
            this.listener = listener;
            this.link = link;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Context context = contextRef.get();
            if (context == null || listener == null) {
                cancel(true);
            } else {

            }
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Context context = contextRef.get();
                if (context == null) {
                    cancel(true);
                    return null;
                }

                mVers = getAppVers(context, link);
                if (mVers == null) {
                    // Fichier de reference injoignable ou illisible : on ne derange pas l'agent.
                    LogUtils.e("Controle de version : fichier de reference illisible - " + link);
                    return false;
                }

                Long publiee = lireVersionCode(mVers.getLatestVersionCode());
                if (publiee == null) return false;

                long installee = Commons.Companion.getAppCuVesCod(context);

                // La mise a jour n'est proposee que si la version publiee est PLUS RECENTE que
                // celle installee. La comparaison testait auparavant l'EGALITE : un appareil en
                // avance sur le fichier de reference - livraison publiee avant la mise a jour du
                // fichier, ou version de test - se voyait reclamer une "mise a jour" vers une
                // version plus ancienne que la sienne.
                boolean verdict = installee < publiee;

                // La liste des versions a ne pas solliciter est facultative dans le fichier. Elle
                // arrive alors a null : Gson construit l'objet sans passer par le constructeur, la
                // valeur par defaut declaree cote Kotlin n'est donc pas appliquee.
                List<Integer> ignorees = mVers.getIgnoreVersionCode();
                if (ignorees != null) {
                    for (Integer vers : ignorees) {
                        if (vers != null && installee == vers.longValue()) verdict = false;
                    }
                }

                LogUtils.d("Controle de version : installee " + installee + ", publiee " + publiee
                        + ", mise a jour proposee : " + verdict);

                return verdict;
            } catch (Exception ex) {
                LogUtils.e("Controle de version : " + ex.getMessage());
                cancel(true);
                return null;
            }
        }

        /** Numero de version publie, ou null s'il est absent ou illisible. */
        private Long lireVersionCode(String valeur) {
            if (valeur == null || valeur.trim().isEmpty()) {
                LogUtils.e("Controle de version : aucun numero de version publie");
                return null;
            }
            try {
                return Long.valueOf(valeur.trim());
            } catch (NumberFormatException ex) {
                LogUtils.e("Controle de version : numero publie illisible - " + valeur);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Boolean update) {
            super.onPostExecute(update);
//            LogUtils.d(link, update);
            if (update != null) {
                if (update) {
                    listener.onCompleted(mVers, true);
                } else {
                    listener.onCompleted(null, false);
                }
            }else listener.onCompleted(null, false);
        }
    }

    public static MissVersion getAppVers(Context context, String link) {
        OkHttpClient client = new OkHttpClient();
        URL url = null;
        try {
            url = new URL(link);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        Request request = new Request.Builder()
                .url(url)
                .build();
        ResponseBody body = null;

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                Gson gson = new Gson();
                MissVersion missVersion = gson.fromJson(response.body().string(), MissVersion.class);
                // Traiter l'objet MissVersion ici
                return missVersion;
            }else return null;

        } catch (IOException ignore) {
            Log.e("Error", "App wasn't found");

        } finally {
            if (body != null) {
                body.close();
            }
        }

        return null;
    }

    public interface LibraryListener {
        void onCompleted(MissVersion vers, Boolean iscomplete);

    }

}
