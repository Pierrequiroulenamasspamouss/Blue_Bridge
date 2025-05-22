from database_manager import DatabaseManager
import json
import re
from datetime import datetime

db = DatabaseManager()

def get_db_connection():
    return db._get_connection('wells')

def get_all_wells():
    return db.wells().get_all_wells()

def get_well_by_id(well_id):
    return db.wells().get_well(well_id)

def get_well_by_esp_id(esp_id):
    return db.wells().get_well_by_esp_id(esp_id)

def update_well_data(well_id, data):
    return db.wells().update_well(well_id, {
        'water_level': data.get('water_level'),
        'water_quality': json.dumps(data.get('water_quality')),
        'status': data.get('status'),
        'last_update': datetime.now().isoformat()
    })

def add_well(well_data):
    return db.wells().create_well({
        'name': well_data.get('name'),
        'description': well_data.get('description'),
        'latitude': well_data.get('latitude'),
        'longitude': well_data.get('longitude'),
        'water_level': well_data.get('water_level'),
        'water_quality': json.dumps(well_data.get('water_quality')),
        'status': well_data.get('status'),
        'owner': well_data.get('owner'),
        'contact_info': well_data.get('contact_info'),
        'access_info': well_data.get('access_info'),
        'notes': well_data.get('notes'),
        'espId': well_data.get('espId'),
        'wellWaterConsumption': well_data.get('wellWaterConsumption'),
        'wellWaterType': well_data.get('wellWaterType')
    })

def delete_well(well_id):
    return db.wells().delete_well(well_id)

def update_well_field(well_id, field_name, new_value):
    return db.wells().update_well(well_id, {field_name: new_value})

def parse_indices(input_str, list_len):
    input_str = input_str.strip().lower()
    indices = set()

    if input_str == "all":
        return list(range(list_len))
    elif input_str.startswith("last"):
        if input_str == "last":
            return [list_len - 1] if list_len > 0 else []
        match = re.match(r"last-(\d+)", input_str)
        if match:
            count = int(match.group(1))
            return list(range(list_len - count, list_len)) if count <= list_len else list(range(list_len))
        else:
            raise ValueError("Invalid 'last-x' format.")
    else:
        parts = input_str.split(',')
        for part in parts:
            if '-' in part:
                start, end = part.split('-')
                indices.update(range(int(start), int(end) + 1))
            else:
                indices.add(int(part))
    return sorted(i for i in indices if 0 <= i < list_len)

def main():
    wells = get_all_wells()

    if not wells:
        print("No wells found.")
        return

    print("\nWells:")
    for idx, well in enumerate(wells):
        print(f"{idx}: ID={well['id']} Name={well['name']}")

    try:
        selection = int(input("\nEnter the number of the well to edit: ").strip())
        well = wells[selection]
    except (ValueError, IndexError):
        print("Invalid selection.")
        return

    print("\nWell data:")
    for key, value in well.items():
        print(f"{key}: {value}")

    try:
        field_name = input("\nEnter field name to edit: ").strip()
        if field_name not in well:
            print("Invalid field name.")
            return
    except (ValueError, IndexError):
        print("Invalid field name.")
        return

    value = well[field_name]
    try:
        parsed = json.loads(value)
        if not isinstance(parsed, list):
            print("The field is not a JSON list.")
            return
    except Exception:
        print("The field does not contain valid JSON.")
        return

    print(f"\nCurrent list ({len(parsed)} items):")
    for i, item in enumerate(parsed):
        print(f"  {i}: {item}")

    delete_input = input("\nEnter indices to delete (e.g., 1,2,4-6, last-2, all): ").strip()
    try:
        to_delete = parse_indices(delete_input, len(parsed))
    except Exception as e:
        print(f"Invalid format: {e}")
        return

    for i in reversed(to_delete):
        removed = parsed.pop(i)
        print(f"Deleted index {i}: {removed}")

    update_well_field(well['id'], field_name, json.dumps(parsed))

if __name__ == "__main__":
    main()
