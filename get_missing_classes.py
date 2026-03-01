import xml.etree.ElementTree as ET

def get_missing_classes(xml_file):
    tree = ET.parse(xml_file)
    root = tree.getroot()

    missing = []

    for package in root.findall('package'):
        for cls in package.findall('class'):
            for counter in cls.findall('counter'):
                if counter.get('type') == 'INSTRUCTION':
                    missed = int(counter.get('missed'))
                    if missed > 0:
                        missing.append(cls.get('name'))

    return missing

print(get_missing_classes('app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml'))
