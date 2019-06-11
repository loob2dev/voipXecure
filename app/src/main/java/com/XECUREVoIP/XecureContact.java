package com.XECUREVoIP;

import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.XECUREVoIP.contacts.ContactDBHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class XecureContact implements Serializable, Comparable<XecureContact> {

    private long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private List<XecureNumberOrAddress> addresses;
    private String company;
    private String department;
    private boolean hasPhoto = false;
    private boolean hasSipAddress;
    private String photoUri;
    private String email;
    private String sub_department;
    private boolean bShare;

    public XecureContact() {
        id = -1;
        firstName = null;
        lastName = null;
        fullName = null;
        email = null;
        company = null;
        photoUri = null;
        addresses = new ArrayList<XecureNumberOrAddress>();
        hasSipAddress = false;
        bShare = false;
    }

    public long getId(){
        return id;
    }

    public void setFirstNameAndLastName(String fn, String ln) {
        if (fn != null && fn.length() == 0 && ln != null && ln.length() == 0) return;

        firstName = fn;
        lastName = ln;
        if (firstName != null && lastName != null && firstName.length() > 0 && lastName.length() > 0) {
            fullName = firstName + " " + lastName;
        } else if (firstName != null && firstName.length() > 0) {
            fullName = firstName;
        } else if (lastName != null && lastName.length() > 0) {
            fullName = lastName;
        }
    }

    public void setEmail(String e) {
        email = e;
    }

    public void setPhoto(byte[] photoToAdd, Context context) {
        try {
            ContextWrapper cw = new ContextWrapper(context);
            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
            File mypath = new File(directory, fullName);
            FileOutputStream fos = new FileOutputStream(mypath);
            fos.write(photoToAdd);
            fos.close();
            hasPhoto = true;
            photoUri = Uri.fromFile(mypath).toString();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void setShare(boolean b){
        bShare = b;
    }

    public boolean isShared(){
        return bShare;
    }

    public void addOrUpdateNumberOrAddress(XecureNumberOrAddress noa) {
        if (noa != null && noa.getValue() != null) {
            if (noa.isSIPAddress()) {
                if (!noa.getValue().startsWith("sip:")) {
                    noa.setValue("sip:" + noa.getValue());
                }
            }
            if (noa.getOldValue() != null) {
                if (noa.isSIPAddress()) {
                    if (!noa.getOldValue().startsWith("sip:")) {
                        noa.setOldValue("sip:" + noa.getOldValue());
                    }
                }
                for (XecureNumberOrAddress address : addresses) {
                    if (noa.getOldValue().equals(address.getValue()) && noa.isSIPAddress() == address.isSIPAddress()) {
                        address.setValue(noa.getValue());
                        break;
                    }
                }
            } else {
                addresses.add(noa);
            }
        }
    }

    public void setCompany(String c) {
        company = c;
    }

    public void setDepartment(String depart){
        department = depart;
    }

    public void setSubDepartment(String sub_dpt) {
        sub_department = sub_dpt;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public Uri getPhotoUri() {
        if (photoUri == null)
            return null;
        return Uri.parse(photoUri);
    }

    public String getCompany() {
        return company;
    }

    public String getDepartment() {
        return department;
    }

    public String getSubDepartment(){
        return sub_department;
    }

    public void removeNumberOrAddress(XecureNumberOrAddress noa) {
        if (noa != null && noa.getOldValue() != null) {
            if (noa.isSIPAddress()) {
                if (!noa.getOldValue().startsWith("sip:")) {
                    noa.setOldValue("sip:" + noa.getOldValue());
                }
            }
            XecureNumberOrAddress toRemove = null;
            for (XecureNumberOrAddress address : addresses) {
                if (noa.getOldValue().equals(address.getValue()) && noa.isSIPAddress() == address.isSIPAddress()) {
                    toRemove = address;
                    break;
                }
            }
            if (toRemove != null) {
                addresses.remove(toRemove);
            }
        }
    }

    public List<XecureNumberOrAddress> getNumbersOrAddresses() {
        return addresses;
    }

    public boolean hasPhoto() {
        if(photoUri == null)
            return false;
        return photoUri.toString().isEmpty()? false : true;
    }

    public Uri getThumbnailUri() {
        if(photoUri == null)
            return null;
        return Uri.parse(photoUri);
    }

    public void clearAddresses() {
        addresses.clear();
    }

    public void setFullName(String displayName) {
        fullName = displayName;
    }

    @Override
    public int compareTo(@NonNull XecureContact contact) {
        String fullName = getFullName() != null ? getFullName().toUpperCase(Locale.getDefault()) : "";
        String contactFullName = contact.getFullName() != null ? contact.getFullName().toUpperCase(Locale.getDefault()) : "";
		/*String firstLetter = fullName == null || fullName.isEmpty() ? "" : fullName.substring(0, 1).toUpperCase(Locale.getDefault());
		String contactfirstLetter = contactFullName == null || contactFullName.isEmpty() ? "" : contactFullName.substring(0, 1).toUpperCase(Locale.getDefault());*/
        return fullName.compareTo(contactFullName);
    }

    public void addNumberOrAddress(XecureNumberOrAddress noa) {
        if (noa == null) return;
        if (noa.isSIPAddress()) {
            hasSipAddress = true;
        }
        addresses.add(noa);
    }

    public String getPresenceModelForUri(String to) {
        return null;
    }

    public void setID(long index) {
        id = index;
    }

    public void setPhotoUri(String strUri) {
        photoUri = strUri;
    }

    public boolean update(Context context) {
        ArrayList<String> arrayAdress = new ArrayList<String>();
        ArrayList<String> arrayNumber = new ArrayList<String>();
        for (XecureNumberOrAddress noa : addresses){
            if (noa.isSIPAddress())
                arrayAdress.add(noa.getValue());
            else
                arrayNumber.add(noa.getValue());
        }
        String address00 = null;
        String number00 = null;
        String number01 = null;
        if (arrayAdress.size() == 1){
            address00 = arrayAdress.get(0);
        }
        if (arrayNumber.size() == 1){
            number00 = arrayNumber.get(0);
        }
        else if (arrayNumber.size() == 2){
            number00 = arrayNumber.get(0);
            number01 = arrayNumber.get(1);
        }
        ContactDBHelper dbHelper = new ContactDBHelper(context);
        id = dbHelper.updateData(
                new Long(id).toString(),
                lastName,
                firstName,
                email,
                address00,
                number00,
                number01,
                photoUri,
                company,
                department,
                sub_department,
                bShare);
        return id ==-1? false : true;
    }

    public boolean save(Context context) {
        ArrayList<String> arrayAdress = new ArrayList<String>();
        ArrayList<String> arrayNumber = new ArrayList<String>();
        for (XecureNumberOrAddress noa : addresses){
            if (noa.isSIPAddress())
                arrayAdress.add(noa.getValue());
            else
                arrayNumber.add(noa.getValue());
        }
        String address00 = null;
        String number00 = null;
        String number01 = null;
        if (arrayAdress.size() == 1){
            address00 = arrayAdress.get(0);
        }
        if (arrayNumber.size() == 1){
            number00 = arrayNumber.get(0);
        }
        else if (arrayNumber.size() == 2){
            number00 = arrayNumber.get(0);
            number01 = arrayNumber.get(1);
        }
        ContactDBHelper dbHelper = new ContactDBHelper(context);
        id = dbHelper.insertData(
                lastName,
                firstName,
                email,
                address00,
                number00,
                number01,
                photoUri,
                company,
                department,
                sub_department,
                bShare);
        return id == -1? false : true;
    }
}
