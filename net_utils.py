from PIL import Image, ImageEnhance
import numpy as np
import os

# Opens a jpg as a 3-color array
def open_jpg(path, row, col):
    im = Image.open(path)
    data = np.asarray(im.getdata())
    full_data = data.reshape((row, col, 3))
    return {"rgb": full_data}

# Opens a jpg as a 3-color array with a certain amount of rotation
def open_rotated_jpg(path, row, col, rot):
    im = Image.open(path)
    im = im.rotate(rot)
    data = np.asarray(im.getdata())
    full_data = data.reshape((row, col, 3))
    return {"rgb": full_data}

# Opens a jpg as a 3-color array with a certain amount of rotation and saturation
def open_rotated_and_saturated_jpg(path, row, col, rot, sat, bright, cont):
    im = Image.open(path)
    im = im.rotate(rot)
    converter = ImageEnhance.Color(im)
    im = converter.enhance(sat)
    converter = ImageEnhance.Brightness(im)
    im = converter.enhance(bright)
    converter = ImageEnhance.Contrast(im)
    im = converter.enhance(cont)
    data = np.asarray(im.getdata())
    full_data = data.reshape((row, col, 3))
    return {"rgb": full_data}

# Opens a jpg as a 3-color array plus a label
def open_labeled_jpg(path, row, col, label):
    jpg = open_jpg(path, row, col)
    jpg["label"] = label
    return jpg

# Bunch of folder-index mappings
label_mappings = ["Apple Braeburn", "Apple Golden 1", "Apple Golden 2",
                  "Apple Golden 3", "Apple Granny Smith", "Apple Red 1", "Apple Red 2",
                  "Apple Red 3", "Apple Red Delicious", "Apple Red Yellow", "Apricot", "Avocado",
                  "Avocado ripe", "Banana", "Banana Red", "Cactus fruit", "Cantaloupe 1",
                  "Cantaloupe 2", "Carambula", "Cherry 1", "Cherry 2", "Cherry Rainier",
                  "Cherry Wax Black", "Cherry Wax Red", "Cherry Wax Yellow", "Clementine", "Cocos", "Dates",
                  "Granadilla", "Grape Pink", "Grape White", "Grape White 2", "Grapefruit Pink",
                  "Grapefruit White", "Guava", "Huckleberry", "Kaki", "Kiwi", "Kumquats", "Lemon",
                  "Lemon Meyer", "Limes", "Lychee", "Mandarine", "Mango", "Maracuja", "Melon Piel de Sapo",
                  "Mulberry", "Nectarine", "Orange", "Papaya", "Passion Fruit", "Peach", "Peach Flat",
                  "Pear", "Pear Abate", "Pear Monster", "Pear Williams", "Pepino",
                  "Physalis", "Physalis with Husk", "Pineapple", "Pineapple Mini",
                  "Pitahaya Red", "Plum", "Pomegranate", "Quince", "Rambutan", "Raspberry", "Salak", "Strawberry",
                  "Strawberry Wedge", "Tamarillo", "Tangelo", "Walnut"]
print("Label mapping count:", len(label_mappings))
print(label_mappings[70])

def index_to_label(index):
    return label_mappings[index]


def label_to_index(label):
    return label_mappings.index(label)


def fetch_next_file_and_label(directory):
    count = 0
    def path_tail(p):
        return os.path.basename(os.path.normpath(p))

    for path, dirs, files in os.walk(directory):
        for f in files:
            if count % 128 == 0:
                pass
            count += 1
            yield os.path.join(path, f), label_to_index(path_tail(path))