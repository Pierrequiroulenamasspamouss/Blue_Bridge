import xml.etree.ElementTree as ET
from xml.dom import minidom

def create_translation_template(source_file, target_file, lang_code='fr'):
    # Parse the source XML file
    tree = ET.parse(source_file)
    root = tree.getroot()

    # Create a new root element for the translation file
    new_root = ET.Element('resources')

    # Process each string element
    for child in root:
        # Copy all attributes
        attrib = child.attrib

        # For translatable="false" strings, keep the original value
        if attrib.get('translatable', 'true') == 'false':
            new_element = ET.SubElement(new_root, child.tag, attrib=attrib)
            new_element.text = child.text
        else:
            # For translatable strings, create empty translation
            new_element = ET.SubElement(new_root, child.tag, attrib=attrib)
            new_element.text = ''  # Empty value for translation

    # Convert to pretty XML
    rough_string = ET.tostring(new_root, 'utf-8')
    reparsed = minidom.parseString(rough_string)
    pretty_xml = reparsed.toprettyxml(indent='    ', encoding='utf-8')

    # Write to file
    with open(target_file, 'wb') as f:
        f.write(pretty_xml)

    print(f"Created translation template with {len(root)} entries at {target_file}")

# Example usage
if __name__ == '__main__':
    source_file = r'C:\Users\Pierr\StudioProjects\Blue_Bridge\app\src\main\res\values\strings.xml'  # Your English strings.xml
    target_file = r'C:\Users\Pierr\StudioProjects\Blue_Bridge\app\src\main\res\values-fr\strings.xml'  # Output French template

    create_translation_template(source_file, target_file)