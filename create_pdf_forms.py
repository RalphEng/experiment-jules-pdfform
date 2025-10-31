#!/usr/bin/env python3
"""
Script to create fillable PDF forms as required by the test specification.
"""

from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import letter
from reportlab.pdfbase import pdfform
from reportlab.lib.colors import black, white

def create_simple_form():
    """Create sample-form-simple.pdf with basic text fields"""
    filename = "/home/engelmann/dev/experiment-pdf-form/src/test/resources/pdfs/sample-form-simple.pdf"
    c = canvas.Canvas(filename, pagesize=letter)
    width, height = letter
    
    # Title
    c.setFont("Helvetica-Bold", 16)
    c.drawString(50, height - 50, "Simple Form")
    
    # Form fields
    y_pos = height - 100
    field_height = 20
    field_width = 200
    spacing = 40
    
    # firstName field
    c.setFont("Helvetica", 12)
    c.drawString(50, y_pos, "First Name:")
    c.acroForm.textfield(name='firstName', tooltip='Enter your first name',
                        x=150, y=y_pos-5, borderStyle='inset',
                        width=field_width, height=field_height)
    
    y_pos -= spacing
    # lastName field
    c.drawString(50, y_pos, "Last Name:")
    c.acroForm.textfield(name='lastName', tooltip='Enter your last name',
                        x=150, y=y_pos-5, borderStyle='inset',
                        width=field_width, height=field_height)
    
    y_pos -= spacing
    # email field
    c.drawString(50, y_pos, "Email:")
    c.acroForm.textfield(name='email', tooltip='Enter your email address',
                        x=150, y=y_pos-5, borderStyle='inset',
                        width=field_width, height=field_height)
    
    y_pos -= spacing
    # phone field
    c.drawString(50, y_pos, "Phone:")
    c.acroForm.textfield(name='phone', tooltip='Enter your phone number',
                        x=150, y=y_pos-5, borderStyle='inset',
                        width=field_width, height=field_height)
    
    y_pos -= spacing
    # address field (multiline)
    c.drawString(50, y_pos, "Address:")
    c.acroForm.textfield(name='address', tooltip='Enter your address',
                        x=150, y=y_pos-25, borderStyle='inset',
                        width=field_width, height=field_height*2)
    
    c.save()
    print(f"Created {filename}")

def create_complex_form():
    """Create sample-form-complex.pdf with multiple field types"""
    filename = "/home/engelmann/dev/experiment-pdf-form/src/test/resources/pdfs/sample-form-complex.pdf"
    c = canvas.Canvas(filename, pagesize=letter)
    width, height = letter
    
    # Title
    c.setFont("Helvetica-Bold", 16)
    c.drawString(50, height - 50, "Complex Form")
    
    y_pos = height - 100
    field_height = 20
    field_width = 200
    spacing = 40
    
    # name field
    c.setFont("Helvetica", 12)
    c.drawString(50, y_pos, "Name:")
    c.acroForm.textfield(name='name', tooltip='Enter your name',
                        x=150, y=y_pos-5, borderStyle='inset',
                        width=field_width, height=field_height)
    
    y_pos -= spacing
    # email field
    c.drawString(50, y_pos, "Email:")
    c.acroForm.textfield(name='email', tooltip='Enter your email',
                        x=150, y=y_pos-5, borderStyle='inset',
                        width=field_width, height=field_height)
    
    y_pos -= spacing
    # subscribe checkbox
    c.drawString(50, y_pos, "Subscribe to newsletter:")
    c.acroForm.checkbox(name='subscribe', tooltip='Check to subscribe',
                       x=200, y=y_pos-5, buttonStyle='check',
                       borderColor=black, fillColor=white,
                       textColor=black, forceBorder=True)
    
    y_pos -= spacing
    # gender radio group
    c.drawString(50, y_pos, "Gender:")
    c.acroForm.radio(name='gender', tooltip='Select gender',
                    value='Male', selected=False,
                    x=150, y=y_pos-5, buttonStyle='circle',
                    borderColor=black, fillColor=white,
                    textColor=black, forceBorder=True)
    c.drawString(170, y_pos, "Male")
    
    c.acroForm.radio(name='gender', tooltip='Select gender',
                    value='Female', selected=False,
                    x=220, y=y_pos-5, buttonStyle='circle',
                    borderColor=black, fillColor=white,
                    textColor=black, forceBorder=True)
    c.drawString(240, y_pos, "Female")
    
    c.acroForm.radio(name='gender', tooltip='Select gender',
                    value='Other', selected=False,
                    x=290, y=y_pos-5, buttonStyle='circle',
                    borderColor=black, fillColor=white,
                    textColor=black, forceBorder=True)
    c.drawString(310, y_pos, "Other")
    
    y_pos -= spacing
    # department dropdown - using a simple textfield due to ReportLab limitations
    c.drawString(50, y_pos, "Department (HR/Engineering/Marketing/Sales/Finance):")
    c.acroForm.textfield(name='department', tooltip='Enter department',
                        x=50, y=y_pos-25, borderStyle='inset',
                        width=field_width, height=field_height)
    
    y_pos -= spacing*2
    # comments field (multiline)
    c.drawString(50, y_pos, "Comments:")
    c.acroForm.textfield(name='comments', tooltip='Enter comments',
                        x=150, y=y_pos-25, borderStyle='inset',
                        width=field_width, height=field_height*3)
    
    c.save()
    print(f"Created {filename}")

def create_special_chars_form():
    """Create sample-form-special-chars.pdf with special character field names"""
    filename = "/home/engelmann/dev/experiment-pdf-form/src/test/resources/pdfs/sample-form-special-chars.pdf"
    c = canvas.Canvas(filename, pagesize=letter)
    width, height = letter
    
    # Title
    c.setFont("Helvetica-Bold", 16)
    c.drawString(50, height - 50, "Special Characters Form")
    
    y_pos = height - 100
    field_height = 20
    field_width = 200
    spacing = 40
    
    # user.name field
    c.setFont("Helvetica", 12)
    c.drawString(50, y_pos, "User Name (field: user.name):")
    c.acroForm.textfield(name='user.name', tooltip='Field with dot in name',
                        x=50, y=y_pos-25, borderStyle='inset',
                        width=field_width, height=field_height)
    
    y_pos -= spacing*2
    # field=value field
    c.drawString(50, y_pos, "Field Value (field: field=value):")
    c.acroForm.textfield(name='field=value', tooltip='Field with equals sign in name',
                        x=50, y=y_pos-25, borderStyle='inset',
                        width=field_width, height=field_height)
    
    y_pos -= spacing*2
    # path\\to\\file field
    c.drawString(50, y_pos, "File Path (field: path\\\\to\\\\file):")
    c.acroForm.textfield(name='path\\to\\file', tooltip='Field with backslashes in name',
                        x=50, y=y_pos-25, borderStyle='inset',
                        width=field_width, height=field_height)
    
    c.save()
    print(f"Created {filename}")

if __name__ == "__main__":
    # Create all three PDF forms
    create_simple_form()
    create_complex_form()
    create_special_chars_form()
    print("All PDF forms created successfully!")