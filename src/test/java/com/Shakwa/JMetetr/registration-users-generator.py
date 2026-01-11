#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script to generate CSV file with test users for JMeter Registration Load Test
Usage: python registration-users-generator.py [number_of_users]
"""

import csv
import sys

def generate_users_csv(num_users=100, filename='registration-users.csv'):
    """
    Generate CSV file with test users for registration
    
    Args:
        num_users: Number of users to generate (default: 100)
        filename: Output CSV filename (default: registration-users.csv)
    """
    
    # Arabic first names
    first_names = ['aa', 'bb', 'cc', 'dd', 'ee', 'ff', 'gg', 'hh', 'ii', 'jj']
    
    # Arabic last names
    last_names = ['aa', 'bb', 'cc', 'dd', 'ee', 'ff', 'gg', 'hh', 'ii', 'jj']
    
    # Standard password for all test users
    password = 'Password123!'
    
    with open(filename, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        
        # Write header
        writer.writerow(['firstName', 'lastName', 'email', 'password'])
        
        # Generate users
        for i in range(1, num_users + 1):
            first_name = first_names[i % len(first_names)]
            last_name = last_names[i % len(last_names)]
            import time
            timestamp = int(time.time())
            email = f'user{i}_{timestamp}@test.com'
            
            writer.writerow([first_name, last_name, email, password])
    
    print(f"✅ Generated {num_users} users in {filename}")

if __name__ == '__main__':
    # Get number of users from command line argument, default to 100
    num_users = 100
    if len(sys.argv) > 1:
        try:
            num_users = int(sys.argv[1])
        except ValueError:
            print("❌ Error: Number of users must be an integer")
            sys.exit(1)
    
    generate_users_csv(num_users)
    print(f"📝 File ready to use in JMeter CSV Data Set Config")

