import argparse
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt

def parse_coords_file(file_path):
    coords = []
    with open(file_path, 'r') as file:
        for line in file:
            parts = line.split('%')
            coords.append(tuple(map(float, parts[0].strip()[1:-1].split(','))))
    return coords

def generate_graph(coords, title, x_label, y_label, outfile):
    # Insert (0, 0) at the beginning of the coordinate list
    coords.insert(0, (0, 0))

    df = pd.DataFrame(coords, columns=['x', 'y'])

    sns.set_style('whitegrid')
    sns.lineplot(x='x', y='y', data=df)
    plt.title(title)
    plt.xlabel(x_label)
    plt.ylabel(y_label)
    plt.savefig(outfile)

def main():
    parser = argparse.ArgumentParser(description='Generate a graph from coordinates')
    parser.add_argument('--coords-file', required=True, help='Path to the coordinates file')
    parser.add_argument('--outfile', required=True, help='Path to the output file (PNG format)')
    parser.add_argument('--title', required=True, help='Title for the graph')
    parser.add_argument('--x-label', required=True, help='X-axis label')
    parser.add_argument('--y-label', required=True, help='Y-axis label')
    args = parser.parse_args()

    coords = parse_coords_file(args.coords_file)
    generate_graph(coords, args.title, args.x_label, args.y_label, args.outfile)

if __name__ == '__main__':
    main()
