import json
import random
import string
from datetime import datetime
from database_manager import DatabaseManager

db = DatabaseManager()

def get_wells():
    """Get all wells with parsed JSON fields"""
    wells = db.wells().get_all_wells()
    for well in wells:
        for field in ['wellLocation', 'waterQuality', 'wellImages']:
            if well.get(field) and isinstance(well[field], str):
                try: well[field] = json.loads(well[field])
                except: well[field] = [] if field == 'wellImages' else None
    return wells

def get_well(wellId):
    """Get single well by ID with parsed JSON"""
    well = db.wells().get_well(wellId)
    if well:
        for field in ['wellLocation', 'waterQuality', 'wellImages']:
            if well.get(field) and isinstance(well[field], str):
                try: well[field] = json.loads(well[field])
                except: well[field] = [] if field == 'wellImages' else None
    return well

def update_well(wellId, data):
    """Update well with JSON serialization"""
    updates = {}
    for k, v in data.items():
        if k in ['wellLocation', 'waterQuality', 'wellImages'] and v:
            updates[k] = json.dumps(v)
        else:
            updates[k] = v
    updates['updatedAt'] = datetime.now().isoformat()
    return db.wells().update_well(wellId, updates)

def add_well(data):
    """Create new well with validation"""
    required = ['wellId', 'wellName', 'wellCapacity', 'wellWaterLevel', 'wellWaterConsumption']
    if any(f not in data for f in required):
        raise ValueError(f"Missing required fields: {required}")

    db_data = {
        'wellId': data['wellId'],
        'wellName': data['wellName'],
        'wellOwner': data.get('wellOwner'),
        'wellWaterType': data.get('wellWaterType', 'Clean'),
        'wellCapacity': float(data['wellCapacity']),
        'wellWaterLevel': float(data['wellWaterLevel']),
        'wellWaterConsumption': float(data['wellWaterConsumption']),
        'wellStatus': data.get('wellStatus', 'Unknown'),
        'updatedAt': datetime.now().isoformat(),
        'createdAt': datetime.now().isoformat(),
        'wellImages': json.dumps(data.get('wellImages', []))
    }

    for field in ['wellLocation', 'waterQuality']:
        if field in data and data[field]:
            db_data[field] = json.dumps(data[field])

    if 'latitude' in data and 'longitude' in data:
        db_data['wellLocation'] = json.dumps({
            'latitude': float(data['latitude']),
            'longitude': float(data['longitude'])
        })

    return db.wells().create_well(db_data)

def random_wells(count=5):
    """Generate random wells"""
    types = ['Clean', 'Mineral', 'Spring', 'Artesian', 'Borehole']
    statuses = ['Active', 'Inactive', 'Maintenance', 'Unknown']
    created = 0

    for _ in range(count):
        well = {
            'wellId': f"ESP-{random.randint(1000,9999)}-{random.choices(string.ascii_uppercase, k=2)}",
            'wellName': f"{random_string()} Well",
            'wellOwner': f"{random_string()} Water Co.",
            'wellLocation': {
                'latitude': round(random.uniform(-90, 90), 6),
                'longitude': round(random.uniform(-180, 180), 6)
            },
            'wellWaterType': random.choice(types),
            'wellCapacity': round(random.uniform(100.0, 1000.0), 2),
            'wellWaterLevel': round(random.uniform(10.0, 100.0), 2),
            'wellWaterConsumption': round(random.uniform(5.0, 100.0), 2),
            'wellStatus': random.choice(statuses),
            'waterQuality': {
                'ph': round(random.uniform(6.0, 8.5), 2),
                'turbidity': round(random.uniform(0.1, 5.0), 2),
                'tds': random.randint(50, 500)
            },
            
        }
        try:
            if add_well(well):
                created += 1
        except Exception as e:
            print(f"Error: {e}")
    print(f"Created {created}/{count} wells")
    return created

def create_well_interactive():
    """Interactive well creation"""
    print("\n=== New Well ===")
    try:
        data = {
            'wellId': input("Well ID: ").strip(),
            'wellName': input("Name: ").strip(),
            'wellOwner': input("Owner: ").strip() or None,
            'latitude': float(input("Latitude: ") or 0),
            'longitude': float(input("Longitude: ") or 0),
            'wellWaterType': input("Water type [Clean/Mineral/Spring/Artesian/Borehole]: ").strip() or 'Clean',
            'wellCapacity': float(input("Capacity: ") or 0),
            'wellWaterLevel': float(input("Water level: ") or 0),
            'wellWaterConsumption': float(input("Consumption: ") or 0),
            'wellStatus': input("Status [Active/Inactive/Maintenance/Unknown]: ").strip() or 'Unknown',
            'waterQuality': {
                'ph': float(input("pH: ") or 7.0),
                'turbidity': float(input("Turbidity: ") or 1.0),
                'tds': int(input("TDS: ") or 100)
            },

        }
        if add_well(data):
            print("✓ Well created!")
            return True
    except Exception as e:
        print(f"✗ Error: {e}")
    return False

def random_string(length=8):
    return ''.join(random.choices(string.ascii_letters, k=length)).capitalize()

def parse_indices(input_str, max_len):
    """Parse index input like '1,2,4-6', 'last-2', or 'all'"""
    input_str = input_str.strip().lower()
    if input_str == "all":
        return list(range(max_len))
    elif input_str.startswith("last"):
        n = 1 if input_str == "last" else int(input_str.split("-")[1])
        return list(range(max(0, max_len - n), max_len))
    indices = set()
    for part in input_str.split(','):
        if '-' in part:
            start, end = map(int, part.split('-'))
            indices.update(range(start, end + 1))
        else:
            indices.add(int(part))
    return sorted(i for i in indices if 0 <= i < max_len)

def main():
    wells = get_wells()
    if not wells and input("No wells. Generate some? (y/n): ").lower() == 'y':
        random_wells(int(input("Count: ") or 5))
        wells = get_wells()

    print(f"\nWells ({len(wells)}):")
    for i, w in enumerate(wells):
        print(f"{i}: {w['wellName']} (ID: {w['wellId']}, Status: {w['wellStatus']})")

    choice = input("\n1. Edit list field\n2. Add well\n3. Random wells\nChoice: ").strip()

    if choice == '1' and wells:
        try:
            well = wells[int(input(f"Well index (0-{len(wells)-1}): "))]
            field = input(f"Field ({[f for f in well if isinstance(well.get(f), str)]}): ").strip()
            if field in well:
                current = json.loads(well[field]) if isinstance(well[field], str) else well[field]
                print(f"\nCurrent {field} ({len(current)} items):")
                for i, item in enumerate(current):
                    print(f"  {i}: {item}")
                to_delete = parse_indices(input("Delete indices: "), len(current))
                for i in reversed(to_delete):
                    current.pop(i)
                update_well(well['wellId'], {field: current})
        except Exception as e:
            print(f"Error: {e}")
    elif choice == '2':
        create_well_interactive()
    elif choice == '3':
        random_wells(int(input("Count: ") or 5))

if __name__ == "__main__":
    main()