package com.example.data.storages.firebase;

import static com.example.data.util.LogTags.DATABASE_ERROR;

import android.graphics.Point;
import android.util.Log;

import com.example.data.storages.models.RestaurantModelData;
import com.example.domain.listeners.GetRestaurantsListener;
import com.example.domain.models.DishModelDomain;
import com.example.domain.models.PointModel;
import com.example.domain.models.RestaurantModelDomain;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.reflect.TypeToken;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RestaurantsStorageImpl implements RestaurantsStorage{

    @Override
    public void getRestaurants(GetRestaurantsListener listener, List<String> dishList, PointModel topLeftPoint, PointModel bottomRightPoint) {




        FirebaseFirestore db = FirebaseFirestore.getInstance();

        CollectionReference restaurantsRef = db.collection("Restaurants");

        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        for (String value : dishList) {
            Task<QuerySnapshot> task1 = restaurantsRef.whereArrayContains("dishesNames", value).get();
            tasks.add(task1);
        }

        Query query = restaurantsRef.whereLessThan("latitude", topLeftPoint.latitude)
                .whereGreaterThan("latitude", bottomRightPoint.latitude);
        Task<QuerySnapshot> task1 = query.get();
        tasks.add(task1);

        Query query1 = restaurantsRef.whereLessThan("longitude", bottomRightPoint.longitude)
                .whereGreaterThan("longitude", topLeftPoint.longitude);
        Task<QuerySnapshot> task2 = query1.get();
        tasks.add(task2);

        Tasks.whenAllSuccess(tasks).addOnCompleteListener(task -> {

            if(task.isSuccessful()){

                List<Object> results = task.getResult();

                List<DocumentSnapshot> combinedResults = new ArrayList<>();

                QuerySnapshot firstQuerySnapshot = (QuerySnapshot) results.get(0);
                combinedResults.addAll(firstQuerySnapshot.getDocuments());

                for (int i = 1; i < results.size(); i++) {
                    QuerySnapshot currentQuerySnapshot = (QuerySnapshot) results.get(i);
                    List<DocumentSnapshot> tempList = new ArrayList<>(currentQuerySnapshot.getDocuments());

                    for (int j = combinedResults.size() - 1; j >= 0; j--) {
                        DocumentSnapshot documentSnapshot = combinedResults.get(j);

                        if (!tempList.contains(documentSnapshot)) {
                            combinedResults.remove(j);
                        }
                    }
                }

                List<RestaurantModelData> restaurantModelDataList = new ArrayList<>();

                for (DocumentSnapshot documentSnapshot : combinedResults){

                    String restaurantName = documentSnapshot.getId();

                    GeoPoint geoPoint = documentSnapshot.getGeoPoint("geoPoint");
                    double latitude = geoPoint.getLatitude();
                    double longitude = geoPoint.getLongitude();

                    PointModel point = new PointModel(latitude, longitude);

                    String json = new Gson().toJson(documentSnapshot.get("dishes"));
                    Type type = new TypeToken<Map<String, Object>>(){}.getType();
                    Map<String, Object> dishesMap = new Gson().fromJson(json, type);
                    List<String> dishNameList = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : dishesMap.entrySet()){
                        dishNameList.add(entry.getKey());
                    }

                    Integer allSum = documentSnapshot.getDouble("allSum").intValue();

                    Integer allCount = documentSnapshot.getDouble("allCount").intValue();

                    String userId = documentSnapshot.getString("userId");

                    RestaurantModelData restaurantModelData = new RestaurantModelData(restaurantName,
                            userId ,allSum, allCount, point, dishNameList);
                    restaurantModelDataList.add(restaurantModelData);
                }

                List<RestaurantModelDomain> restaurantModelDomainList = new ArrayList<>();
                for (RestaurantModelData restaurantModelData : restaurantModelDataList){

                    restaurantModelDomainList.add(new RestaurantModelDomain(restaurantModelData.restaurantName,
                            restaurantModelData.userId, restaurantModelData.allSum, restaurantModelData.allCount, restaurantModelData.geoPoint,
                            restaurantModelData.dishNameList));

                }
                Log.d("FHJFHFHJFHFHJFHFJHFHJFHFHFH", String.valueOf(restaurantModelDomainList.size()));
                listener.getRestaurants(restaurantModelDomainList);
            }
            else{
                Log.w(DATABASE_ERROR, "RestaurantsStorageImpl");
            }

        });

    }

}
