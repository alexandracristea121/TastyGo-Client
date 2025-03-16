import firebase_admin
from firebase_admin import credentials, db
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.neighbors import KNeighborsRegressor
from sklearn.metrics import mean_absolute_error
from sklearn.preprocessing import StandardScaler

# Initialize Firebase
cred = credentials.Certificate('/Users/alexscrofan/Desktop/TastyGo-Client/app/src/main/java/com/examples/licenta_food_ordering/ml_scripts/food-delivery-4dd54-firebase-adminsdk-pm5na-fdeafcb967.json')
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://food-delivery-4dd54-default-rtdb.firebaseio.com'
})

# Function to fetch data from Firebase
def fetch_data_from_firebase():
    ref = db.reference('couriers')
    data = ref.get()

    if not data:
        print("No courier data found in Firebase.")
        exit()

    courier_data = []

    for courier_id, courier_info in data.items():
        courier_data.append({
            'id': courier_id,
            'minDistance': courier_info.get('minDistance', 0),
            'trafficEstimationInMinutes': courier_info.get('trafficEstimationInMinutes', 0),
            'courierLatitude': courier_info.get('latitude', 0.0),
            'courierLongitude': courier_info.get('longitude', 0.0),
            'restaurantLatitude': courier_info.get('restaurantLatitude', 0.0),
            'restaurantLongitude': courier_info.get('restaurantLongitude', 0.0),
            'userLatitude': courier_info.get('userLatitude', 0.0),
            'userLongitude': courier_info.get('userLongitude', 0.0),
            'status': courier_info.get('status', ''),
            'lastUpdate': courier_info.get('lastUpdate', 0)
        })

    return pd.DataFrame(courier_data)

# Fetch data
df = fetch_data_from_firebase()
print(f"Raw Data:\n{df}")

# Remove rows with zero/missing values
df = df[(df['minDistance'] > 0) & (df['trafficEstimationInMinutes'] > 0) &
        (df['restaurantLatitude'] > 0) & (df['restaurantLongitude'] > 0) &
        (df['userLatitude'] > 0) & (df['userLongitude'] > 0)]

print(f"Filtered Data:\n{df}")

if df.shape[0] < 2:
    print("Not enough data for training. Exiting.")
    exit()

# Add time_since_last_update feature
df['time_since_last_update'] = (pd.to_datetime('now') - pd.to_datetime(df['lastUpdate'], unit='ms')).dt.total_seconds()

# Encode 'status'
df['status'] = df['status'].apply(lambda x: 1 if x == 'DELIVERING' else 0)

# Define features and target
X = df[['minDistance', 'trafficEstimationInMinutes', 'courierLatitude', 'courierLongitude',
        'restaurantLatitude', 'restaurantLongitude', 'userLatitude', 'userLongitude', 'status',
        'lastUpdate', 'time_since_last_update']]
y = df['trafficEstimationInMinutes']

# Split data
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
print(f"Training Set: {X_train.shape}, Test Set: {X_test.shape}")

# Scaling
scaler = StandardScaler()
X_train_scaled = scaler.fit_transform(X_train)
X_test_scaled = scaler.transform(X_test)

# Train model
model = KNeighborsRegressor(n_neighbors=3)
model.fit(X_train_scaled, y_train)

# Predict on test set
y_pred = model.predict(X_test_scaled)
mae = mean_absolute_error(y_test, y_pred)
print(f'Mean Absolute Error (MAE): {mae}')

# Predict for each courier
for _, courier_info in df.iterrows():
    features = courier_info[['minDistance', 'trafficEstimationInMinutes', 'courierLatitude', 'courierLongitude',
                             'restaurantLatitude', 'restaurantLongitude', 'userLatitude', 'userLongitude', 'status',
                             'lastUpdate', 'time_since_last_update']].to_numpy().reshape(1, -1)

    features_scaled = scaler.transform(features)
    predicted_time = model.predict(features_scaled)[0]

    print(f"Courier ID: {courier_info['id']}")
    print(f"Predicted Delivery Time: {predicted_time:.2f} minutes")
    print("-" * 50)