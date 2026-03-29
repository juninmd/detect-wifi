import xml.etree.ElementTree as ET
tree = ET.parse('app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml')
root = tree.getroot()

def get_coverage(class_name_contains):
    for pkg in root.findall('package'):
        for cls in pkg.findall('class'):
            if class_name_contains in cls.attrib['name']:
                for counter in cls.findall('counter'):
                    if counter.attrib['type'] == 'INSTRUCTION':
                        missed = int(counter.attrib['missed'])
                        covered = int(counter.attrib['covered'])
                        total = missed + covered
                        print(f"Coverage for {cls.attrib['name']}: {covered}/{total} ({(covered/total)*100:.2f}%)")

get_coverage("NotificationUtil")
