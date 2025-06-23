from database_manager import DatabaseManager
import json
import random
import string
from datetime import datetime

db = DatabaseManager()

def get_all_wells():
    return db.wells().get_all_wells()

def get_well_by_id(well_id):
    return db.wells().get_well(well_id)

def update_well_data(well_id, data):
    return db.wells().update_well(well_id, {
        'wellWaterLevel': data.get('wellWaterLevel'),
        'waterQuality': json.dumps(data.get('waterQuality')),
        'wellStatus': data.get('wellStatus'),
        'lastUpdated': datetime.now().isoformat()
    })

def add_well(well_data):
    print(f"DEBUG: add_well called with keys: {list(well_data.keys())}")

    # Fill missing required fields with dummy data
    if not well_data.get('espId'):
        well_data['espId'] = f"ESP-{random.randint(1000, 9999)}-{''.join(random.choices(string.ascii_uppercase, k=2))}"
        print(f"DEBUG: Generated espId: {well_data['espId']}")
    if not well_data.get('wellName'):
        well_data['wellName'] = f"Auto Well {random.randint(1, 999)}"
        print(f"DEBUG: Generated wellName: {well_data['wellName']}")
    if well_data.get('wellCapacity') is None:
        well_data['wellCapacity'] = round(random.uniform(100.0, 1000.0), 2)
        print(f"DEBUG: Generated wellCapacity: {well_data['wellCapacity']}")
    if well_data.get('wellWaterLevel') is None:
        well_data['wellWaterLevel'] = round(random.uniform(10.0, 100.0), 2)
        print(f"DEBUG: Generated wellWaterLevel: {well_data['wellWaterLevel']}")
    if well_data.get('wellWaterConsumption') is None:
        well_data['wellWaterConsumption'] = round(random.uniform(5.0, 100.0), 2)
        print(f"DEBUG: Generated wellWaterConsumption: {well_data['wellWaterConsumption']}")
    if not well_data.get('wellStatus'):
        well_data['wellStatus'] = 'Unknown'
        print(f"DEBUG: Generated wellStatus: {well_data['wellStatus']}")

    print(f"DEBUG: Final well_data keys: {list(well_data.keys())}")
    print(f"DEBUG: All required field values:")
    required_fields = ['espId', 'wellName', 'wellCapacity', 'wellWaterLevel', 'wellWaterConsumption', 'wellStatus']
    for field in required_fields:
        print(f"  {field}: {well_data.get(field)} (type: {type(well_data.get(field))})")

    # Validate required fields
    for field in required_fields:
        if field not in well_data or well_data[field] is None:
            raise ValueError(f"Missing required field: {field}")

    # Prepare location data
    location = None
    if well_data.get('latitude') and well_data.get('longitude'):
        location = {
            'latitude': well_data['latitude'],
            'longitude': well_data['longitude']
        }

    # Prepare water quality data
    water_quality = well_data.get('waterQuality', {})
    if not isinstance(water_quality, dict):
        water_quality = {}

    # Prepare final data for database - include all possible required fields
    db_data = {
        'espId': well_data['espId'],
        'wellName': well_data['wellName'],
        'wellOwner': well_data.get('wellOwner'),
        'wellLocation': json.dumps(location) if location else None,
        'wellWaterType': well_data.get('wellWaterType', 'Clean'),
        'wellCapacity': float(well_data['wellCapacity']),
        'wellWaterLevel': float(well_data['wellWaterLevel']),
        'wellWaterConsumption': float(well_data['wellWaterConsumption']),
        'waterQuality': json.dumps(water_quality),
        'wellStatus': well_data['wellStatus'],
        'extraData': json.dumps(well_data.get('extraData', {})),
        'lastUpdated': datetime.now().isoformat(),
        'ownerId': well_data.get('ownerId', 1)  # Default to user ID 1
    }

    print(f"DEBUG: Sending to database: {list(db_data.keys())}")
    print(f"DEBUG: Database data values:")
    for key, value in db_data.items():
        print(f"  {key}: {value} (type: {type(value)})")

    try:
        result = db.wells().create_well(db_data)
        print(f"DEBUG: Database create_well returned: {result}")
        return result
    except Exception as e:
        print(f"DEBUG: Database error: {e}")
        print(f"DEBUG: Database error type: {type(e)}")

        # Try with just the absolute minimum fields
        print("DEBUG: Trying with minimal fields...")
        minimal_data = {
            'espId': well_data['espId'],
            'wellName': well_data['wellName'],
            'wellCapacity': float(well_data['wellCapacity']),
            'wellWaterLevel': float(well_data['wellWaterLevel']),
            'wellWaterConsumption': float(well_data['wellWaterConsumption']),
            'wellStatus': well_data['wellStatus']
        }

        try:
            result = db.wells().create_well(minimal_data)
            print(f"DEBUG: Minimal create_well succeeded: {result}")
            return result
        except Exception as e2:
            print(f"DEBUG: Minimal create_well also failed: {e2}")

            # Try adding fields one by one to see which one breaks it
            test_fields = ['wellOwner', 'wellLocation', 'wellWaterType', 'waterQuality', 'extraData', 'lastUpdated', 'ownerId']
            working_data = minimal_data.copy()

            for field in test_fields:
                if field in db_data:
                    working_data[field] = db_data[field]
                    try:
                        print(f"DEBUG: Testing with field '{field}'...")
                        result = db.wells().create_well(working_data.copy())
                        print(f"DEBUG: Success with {field}!")
                        return result
                    except Exception as e3:
                        print(f"DEBUG: Failed with {field}: {e3}")
                        # Remove the field and continue testing
                        del working_data[field]

            raise Exception(f"Could not create well with any field combination. Original error: {e}")

def update_well_field(well_id, field_name, new_value):
    return db.wells().update_well(well_id, {field_name: new_value})

def generate_random_string(length=8):
    return ''.join(random.choices(string.ascii_letters, k=length)).capitalize()

def generate_random_wells(count=5):
    """Generate random well data"""
    water_types = ['Clean', 'Mineral', 'Spring', 'Artesian', 'Borehole']
    statuses = ['Active', 'Inactive', 'Maintenance', 'Unknown']

    created_count = 0
    for i in range(count):
        # Generate all required fields with dummy data
        well_data = {
            'espId': f"ESP-{random.randint(1000, 9999)}-{''.join(random.choices(string.ascii_uppercase, k=2))}",
            'wellName': f"{generate_random_string()} Well {i+1}",
            'wellOwner': f"{generate_random_string()} Water Co.",
            'latitude': round(random.uniform(-90, 90), 6),
            'longitude': round(random.uniform(-180, 180), 6),
            'wellWaterType': random.choice(water_types),
            'wellCapacity': round(random.uniform(100.0, 1000.0), 2),
            'wellWaterLevel': round(random.uniform(10.0, 100.0), 2),
            'wellWaterConsumption': round(random.uniform(5.0, 100.0), 2),
            'wellStatus': random.choice(statuses),
            'waterQuality': {
                'ph': round(random.uniform(6.0, 8.5), 2),
                'turbidity': round(random.uniform(0.1, 5.0), 2),
                'tds': random.randint(50, 500)
            },
            'extraData': {
                'description': f"Well installed in {random.randint(2010, 2023)}",
                'contact': f"{random.randint(100, 999)}-{random.randint(100, 999)}-{random.randint(1000, 9999)}",
                'notes': "Automatically generated test data"
            }
        }

        try:
            print(f"\nAttempting to create well {i+1}:")
            print(f"  ESP ID: {well_data['espId']}")
            print(f"  Name: {well_data['wellName']}")
            print(f"  Capacity: {well_data['wellCapacity']} (type: {type(well_data['wellCapacity'])})")
            print(f"  Water Level: {well_data['wellWaterLevel']} (type: {type(well_data['wellWaterLevel'])})")
            print(f"  Consumption: {well_data['wellWaterConsumption']} (type: {type(well_data['wellWaterConsumption'])})")
            print(f"  Status: {well_data['wellStatus']}")

            result = add_well(well_data)
            print(f"✓ Created: {well_data['wellName']} (DB ID: {result})")
            created_count += 1
        except Exception as e:
            print(f"✗ Failed to create well {i+1}: {e}")
            print(f"  Error type: {type(e)}")
            import traceback
            print(f"  Full traceback: {traceback.format_exc()}")

    print(f"\nSuccessfully created {created_count}/{count} wells")
    return created_count

def parse_indices(input_str, list_len):
    """Parse user input for indices (e.g., '1,2,4-6', 'last-2', 'all')"""
    input_str = input_str.strip().lower()
    indices = set()

    if input_str == "all":
        return list(range(list_len))
    elif input_str.startswith("last"):
        if input_str == "last":
            return [list_len - 1] if list_len > 0 else []
        elif "-" in input_str:
            count = int(input_str.split("-")[1])
            return list(range(max(0, list_len - count), list_len))
    else:
        for part in input_str.split(','):
            if '-' in part:
                start, end = map(int, part.split('-'))
                indices.update(range(start, end + 1))
            else:
                indices.add(int(part))

    return sorted(i for i in indices if 0 <= i < list_len)

def edit_well_list_field(well, field_name):
    """Edit a JSON list field in a well"""
    try:
        parsed = json.loads(well[field_name])
        if not isinstance(parsed, list):
            print("The field is not a JSON list.")
            return False
    except Exception:
        print("The field does not contain valid JSON.")
        return False

    print(f"\nCurrent list ({len(parsed)} items):")
    for i, item in enumerate(parsed):
        print(f"  {i}: {item}")

    delete_input = input("\nEnter indices to delete (e.g., '1,2', '4-6', 'last-2', 'all'): ").strip()
    if not delete_input:
        return False

    try:
        to_delete = parse_indices(delete_input, len(parsed))
        for i in reversed(to_delete):
            removed = parsed.pop(i)
            print(f"Deleted index {i}: {removed}")

        update_well_field(well['id'], field_name, json.dumps(parsed))
        print("Field updated successfully!")
        return True
    except Exception as e:
        print(f"Error: {e}")
        return False

def create_new_well():
    """Interactive well creation"""
    print("\n=== Creating New Well ===")
    try:
        well_data = {
            'espId': input("ESP ID (required): ").strip(),
            'wellName': input("Well name (required): ").strip(),
            'wellOwner': input("Owner (optional): ").strip() or None,
            'latitude': float(input("Latitude: ") or 0),
            'longitude': float(input("Longitude: ") or 0),
            'wellWaterType': input("Water type [Clean/Mineral/Spring/Artesian/Borehole] (default: Clean): ").strip() or 'Clean',
            'wellCapacity': float(input("Capacity (required): ") or 0),
            'wellWaterLevel': float(input("Water level (required): ") or 0),
            'wellWaterConsumption': float(input("Water consumption (required): ") or 0),
            'wellStatus': input("Status [Active/Inactive/Maintenance/Unknown] (default: Unknown): ").strip() or 'Unknown',
            'waterQuality': {
                'ph': float(input("pH level (optional): ") or 7.0),
                'turbidity': float(input("Turbidity (optional): ") or 1.0),
                'tds': int(input("TDS (optional): ") or 100)
            },
            'extraData': {
                'description': input("Description (optional): ").strip() or "",
                'contact': input("Contact info (optional): ").strip() or "",
                'notes': input("Notes (optional): ").strip() or ""
            }
        }

        add_well(well_data)
        print("\n✓ Well created successfully!")
        return True
    except Exception as e:
        print(f"\n✗ Failed to create well: {e}")
        return False

def main():
    wells = get_all_wells()

    # Handle empty database
    if not wells:
        print("No wells found in the database.")
        if input("Generate sample wells? (y/n): ").strip().lower() == 'y':
            count = int(input("How many wells? (default 5): ").strip() or 5)
            if generate_random_wells(count) > 0:
                wells = get_all_wells()
            else:
                print("No wells were created. Exiting.")
                return
        else:
            return

    # Display wells
    print(f"\n=== Wells ({len(wells)}) ===")
    for i, well in enumerate(wells):
        print(f"{i}: {well['wellName']} (ID: {well['id']}, Status: {well['wellStatus']})")

    # Main menu
    print("\n=== Options ===")
    print("1. Edit well list field")
    print("2. Add new well")
    print("3. Generate random wells")

    choice = input("\nChoice (1-3): ").strip()

    if choice == '1':
        try:
            idx = int(input(f"\nSelect well (0-{len(wells)-1}): "))
            well = wells[idx]

            print(f"\n=== {well['wellName']} ===")
            json_fields = [k for k, v in well.items() if k in ['waterQuality', 'extraData', 'wellLocation']]

            if not json_fields:
                print("No JSON list fields available.")
                return

            print("Available JSON fields:", ', '.join(json_fields))
            field = input("Enter field name: ").strip()

            if field in json_fields:
                edit_well_list_field(well, field)
            else:
                print("Invalid field name.")

        except (ValueError, IndexError) as e:
            print(f"Invalid selection: {e}")

    elif choice == '2':
        create_new_well()

    elif choice == '3':
        count = int(input("How many wells to generate? (default 5): ").strip() or 5)
        generate_random_wells(count)

    else:
        print("Invalid choice.")

if __name__ == "__main__":
    main()