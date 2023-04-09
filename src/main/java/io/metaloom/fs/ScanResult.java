package io.metaloom.fs;

import java.util.Set;

public interface ScanResult {

	Set<FileInfo> added();

	Set<FileInfo> present();

	Set<FileInfo> modified();

	Set<FileInfo> moved();

	Set<FileInfo> deleted();

}
